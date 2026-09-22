/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.backoffice.jobs

import com.hybris.backoffice.excel.data.Impex
import com.hybris.backoffice.excel.data.ImpexHeaderValue
import com.hybris.backoffice.excel.importing.ExcelImportService
import com.hybris.backoffice.excel.template.workbook.ExcelWorkbookService
import com.sap.cx.translations.backoffice.model.CxLocalizedValueExcelImportCronJobModel
import com.sap.cx.translations.model.CxLocalizedValueModel
import com.sap.cx.translations.services.CxTranslateCommerceCommonI18NService
import com.sap.cx.translations.services.localizedobjects.CxLocalizedValueService
import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.ItemModel
import de.hybris.platform.core.model.media.MediaModel
import de.hybris.platform.cronjob.jalo.AbstractProgressTracker
import de.hybris.platform.servicelayer.media.MediaService
import de.hybris.platform.servicelayer.model.ModelService
import org.apache.poi.ss.usermodel.Workbook
import spock.lang.Specification
import spock.lang.Subject

/**
 * Unit tests for {@link CxLocalizedValueExcelImportJob}.
 *
 * Covers the row-level fixes (silent-skip → failedRows, duplicate-key drop, missing-target
 * NPE guard, missing-keyColumn fail-fast) and the importLocalizedValue() branches.
 *
 * The full executeImport() flow is exercised end-to-end with the excel layer mocked,
 * so the row loop's failedRows/alreadyProcessedRows accounting is verified through the
 * cronJob setters that fire per row.
 */
@UnitTest
class CxLocalizedValueExcelImportJobSpec extends Specification {

    ModelService modelService = Mock()
    CxLocalizedValueService cxLocalizedValueService = Mock()
    CxTranslateCommerceCommonI18NService i18nService = Mock()
    MediaService mediaService = Mock()
    ExcelWorkbookService excelWorkbookService = Mock()
    ExcelImportService excelImportService = Mock()

    /** Safe no-op tracker — storeProgressInDB is abstract; close() is a no-op here too. */
    AbstractProgressTracker tracker = new AbstractProgressTracker() {
        @Override void storeProgressInDB(Double progress) { /* no-op */ }
        @Override void close() { /* no-op */ }
    }

    /**
     * Job-under-test with createProgressTracker() overridden to return a mock tracker,
     * and clearAbortRequestedIfNeeded forced to false (it's final on AbstractJobPerformable
     * and tries to use the real modelService — so we override the only call site indirectly
     * by stubbing modelService.refresh / getSource as no-ops via Mock defaults).
     */
    @Subject
    CxLocalizedValueExcelImportJob job = new CxLocalizedValueExcelImportJob() {
        @Override
        protected AbstractProgressTracker createProgressTracker(CxLocalizedValueExcelImportCronJobModel cronJob) {
            return tracker
        }
    }

    CxLocalizedValueExcelImportCronJobModel cronJob = Mock()
    MediaModel mediaModel = Mock()

    def setup() {
        job.setCxLocalizedValueService(cxLocalizedValueService)
        job.setCxTranslateCommerceCommonI18NService(i18nService)
        job.setMediaService(mediaService)
        job.setExcelWorkbookService(excelWorkbookService)
        job.setExcelImportService(excelImportService)
        job.modelService = modelService
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ImpexHeaderValue keyHeader() {
        // qualifier must be "key" — production code matches on getQualifier(); use the Builder to set it
        return new ImpexHeaderValue.Builder("key").withQualifier("key").build()
    }

    private ImpexHeaderValue valueHeader(final String lang) {
        // qualifier "value" + lang on each value column
        return new ImpexHeaderValue.Builder("value").withQualifier("value").withLang(lang).build()
    }

    /**
     * Build an Impex containing a single ImpexForType for CxLocalizedValueModel,
     * whose impexTable has the supplied rows (row index → header → cell value).
     *
     * Note: Impex/ImpexForType expose populating APIs (findUpdates/putValue) rather than
     * field setters — the internal Table is private/final.
     */
    private Impex impexWithRows(List<Map<ImpexHeaderValue, Object>> rows) {
        def impex = new Impex()
        def impexForType = impex.findUpdates(CxLocalizedValueModel._TYPECODE)
        rows.eachWithIndex { Map<ImpexHeaderValue, Object> rowMap, int i ->
            rowMap.each { header, value ->
                if (value != null) {
                    impexForType.putValue(i, header, value)
                }
            }
        }
        return impex
    }

    private void wireExcelChain(Impex impex) {
        def workbook = Mock(Workbook)
        cronJob.getExcelFile() >> mediaModel
        mediaService.getStreamFromMedia(mediaModel) >> Mock(InputStream)
        excelWorkbookService.createWorkbook(_) >> workbook
        excelImportService.convertToImpex(workbook) >> impex
    }

    // ------------------------------------------------------------------
    // calculateProgress — covers division-by-zero guard
    // ------------------------------------------------------------------

    def "calculateProgress returns 100 when totalRows is 0"() {
        expect:
        CxLocalizedValueExcelImportJob.calculateProgress(0, 0) == 100d
    }

    def "calculateProgress returns percentage based on processed/total"() {
        expect:
        CxLocalizedValueExcelImportJob.calculateProgress(10, 5) == 50d
        CxLocalizedValueExcelImportJob.calculateProgress(4, 1) == 25d
    }

    // ------------------------------------------------------------------
    // findKeyColumn / findValueColumns
    // ------------------------------------------------------------------

    def "findKeyColumn returns the header with qualifier 'key'"() {
        given:
        def keyCol = keyHeader()
        def valueCol = valueHeader("en")

        expect:
        job.findKeyColumn([keyCol, valueCol] as Set) == keyCol
    }

    def "findKeyColumn returns null when no key column exists"() {
        expect:
        job.findKeyColumn([valueHeader("en")] as Set) == null
    }

    def "findValueColumns returns only headers with qualifier 'value'"() {
        given:
        def keyCol = keyHeader()
        def en = valueHeader("en")
        def de = valueHeader("de")

        expect:
        job.findValueColumns([keyCol, en, de] as Set) == ([en, de] as Set)
    }

    // ------------------------------------------------------------------
    // importLocalizedValue — covers null-target guard and per-locale paths
    // ------------------------------------------------------------------

    def "importLocalizedValue returns false when target is null (no NPE)"() {
        given:
        def cxLv = Mock(CxLocalizedValueModel)
        cxLv.getTarget() >> null

        when:
        def result = job.importLocalizedValue("k", cxLv, [(Locale.ENGLISH): "v"])

        then:
        result == false
        0 * modelService.save(_)
    }

    def "importLocalizedValue returns true when all locales set and save succeeds"() {
        given:
        def target = Mock(ItemModel)
        def cxLv = Mock(CxLocalizedValueModel)
        cxLv.getTarget() >> target
        cxLv.getAttribute() >> "name"

        when:
        def result = job.importLocalizedValue("k", cxLv, [(Locale.ENGLISH): "v_en", (Locale.GERMAN): "v_de"])

        then:
        result == true
        1 * target.setProperty("name", Locale.ENGLISH, "v_en")
        1 * target.setProperty("name", Locale.GERMAN, "v_de")
        1 * modelService.save(target)
    }

    def "importLocalizedValue returns false when setProperty throws for one locale (save still attempted)"() {
        given:
        def target = Mock(ItemModel)
        def cxLv = Mock(CxLocalizedValueModel)
        cxLv.getTarget() >> target
        cxLv.getAttribute() >> "name"
        cxLv.getKey() >> "k"
        target.setProperty("name", Locale.GERMAN, _) >> { throw new RuntimeException("constraint") }

        when:
        def result = job.importLocalizedValue("k", cxLv, [(Locale.ENGLISH): "v_en", (Locale.GERMAN): "v_de"])

        then:
        result == false
        1 * modelService.save(target)
    }

    def "importLocalizedValue returns false when modelService.save throws"() {
        given:
        def target = Mock(ItemModel)
        def cxLv = Mock(CxLocalizedValueModel)
        cxLv.getTarget() >> target
        cxLv.getAttribute() >> "name"
        cxLv.getKey() >> "k"
        modelService.save(target) >> { throw new RuntimeException("db") }

        when:
        def result = job.importLocalizedValue("k", cxLv, [(Locale.ENGLISH): "v"])

        then:
        result == false
    }

    def "importLocalizedValue saves target even when langToValueMap is empty"() {
        given:
        def target = Mock(ItemModel)
        def cxLv = Mock(CxLocalizedValueModel)
        cxLv.getTarget() >> target

        when:
        def result = job.importLocalizedValue("k", cxLv, [:])

        then:
        result == true
        0 * target.setProperty(_, _, _)
        1 * modelService.save(target)
    }

    // ------------------------------------------------------------------
    // executeImport — covers fail-fast branches
    // ------------------------------------------------------------------

    def "executeImport returns false when excel file is missing"() {
        given:
        cronJob.getExcelFile() >> null

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
    }

    def "executeImport returns false when impex contains no CxLocalizedValue table"() {
        given:
        // A fresh Impex has an empty impexes list — production code uses findFirst().orElse(null)
        def empty = new Impex()
        wireExcelChain(empty)

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
    }

    def "executeImport returns false when key column is missing (fail-fast fix)"() {
        given:
        // Only a value column, no key column.
        def valueCol = valueHeader("en")
        def impex = impexWithRows([
                [(valueCol): "v"]
        ])
        wireExcelChain(impex)

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
        // The new code short-circuits BEFORE setting totalRows — confirm no row processing
        0 * cronJob.setAlreadyProcessedRows(_)
    }

    // ------------------------------------------------------------------
    // executeImport — row-loop semantics (the actual fixes)
    // ------------------------------------------------------------------

    def "executeImport returns true when single row imports successfully"() {
        given:
        def keyCol = keyHeader()
        def enCol = valueHeader("en")
        def impex = impexWithRows([
                [(keyCol): "k1", (enCol): "hello"]
        ])
        wireExcelChain(impex)

        def target = Mock(ItemModel)
        def lv = Mock(CxLocalizedValueModel)
        lv.getTarget() >> target
        lv.getAttribute() >> "name"
        cxLocalizedValueService.getForKey("k1") >> [lv]
        i18nService.getLocale("en") >> Locale.ENGLISH

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == true
        1 * target.setProperty("name", Locale.ENGLISH, "hello")
        1 * cronJob.setFailedRows(0)
        1 * cronJob.setAlreadyProcessedRows(1)
    }

    def "executeImport increments failedRows when key is not a String (was silently swallowed before fix)"() {
        given:
        def keyCol = keyHeader()
        def impex = impexWithRows([
                [(keyCol): 42 as Integer]
        ])
        wireExcelChain(impex)

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
        1 * cronJob.setFailedRows(1)
        1 * cronJob.setAlreadyProcessedRows(1)
        0 * cxLocalizedValueService.getForKey(_)
    }

    def "executeImport increments failedRows when key column value is blank"() {
        given:
        def keyCol = keyHeader()
        def impex = impexWithRows([
                [(keyCol): "   "]
        ])
        wireExcelChain(impex)

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
        1 * cronJob.setFailedRows(1)
        0 * cxLocalizedValueService.getForKey(_)
    }

    def "executeImport increments failedRows and skips import when key matches multiple records (duplicate-key fix)"() {
        given:
        def keyCol = keyHeader()
        def impex = impexWithRows([
                [(keyCol): "dupe"]
        ])
        wireExcelChain(impex)

        def lv1 = Mock(CxLocalizedValueModel)
        def lv2 = Mock(CxLocalizedValueModel)
        cxLocalizedValueService.getForKey("dupe") >> [lv1, lv2]

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
        1 * cronJob.setFailedRows(1)
        // Critically: neither record is silently used — both targets untouched.
        0 * lv1.getTarget()
        0 * lv2.getTarget()
    }

    def "executeImport increments failedRows when no record is found for key"() {
        given:
        def keyCol = keyHeader()
        def impex = impexWithRows([
                [(keyCol): "missing"]
        ])
        wireExcelChain(impex)
        cxLocalizedValueService.getForKey("missing") >> []

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
        1 * cronJob.setFailedRows(1)
    }

    def "executeImport processes mixed-validity rows correctly (counters align)"() {
        given:
        def keyCol = keyHeader()
        def enCol = valueHeader("en")
        def impex = impexWithRows([
                [(keyCol): "ok",     (enCol): "v_en"],   // success
                [(keyCol): 99,       (enCol): "v_en"],   // failed: non-String key
                [(keyCol): "absent", (enCol): "v_en"],   // failed: getForKey returns empty
                [(keyCol): "good2",  (enCol): "v_en2"]   // success
        ])
        wireExcelChain(impex)

        def target = Mock(ItemModel)
        def lvOk = Mock(CxLocalizedValueModel)
        lvOk.getTarget() >> target
        lvOk.getAttribute() >> "name"
        def lvGood2 = Mock(CxLocalizedValueModel)
        lvGood2.getTarget() >> target
        lvGood2.getAttribute() >> "name"

        cxLocalizedValueService.getForKey("ok") >> [lvOk]
        cxLocalizedValueService.getForKey("absent") >> []
        cxLocalizedValueService.getForKey("good2") >> [lvGood2]
        i18nService.getLocale("en") >> Locale.ENGLISH

        when:
        def result = job.executeImport(cronJob, tracker)

        then:
        result == false
        // After the last row, the cumulative counters are: failedRows=2, alreadyProcessedRows=4.
        // setFailedRows(2) fires on rows 3 and 4 (cumulative stays at 2), so we use ≥1 here.
        (1.._) * cronJob.setFailedRows(2)
        1 * cronJob.setAlreadyProcessedRows(4)
    }

    // ------------------------------------------------------------------
    // Contract checks
    // ------------------------------------------------------------------

    def "isAbortable returns true"() {
        expect:
        job.isAbortable()
    }

    def "createProgressTracker can be overridden in subclass (testability anchor)"() {
        expect:
        // Sanity: the overridden factory returns our no-op tracker, not a CronJobProgressTracker.
        job.createProgressTracker(cronJob).is(tracker)
    }
}
