/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.cx.translations.aspects

import com.sap.cx.translations.messages.CxDatabaseMessageSource
import de.hybris.bootstrap.annotations.UnitTest
import org.aspectj.lang.ProceedingJoinPoint
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceResolvable
import org.springframework.context.NoSuchMessageException
import spock.lang.Specification
import spock.lang.Subject

import java.lang.reflect.Method

/**
 * Unit tests for {@link CxMessageSourceAspect}
 */
@UnitTest
class CxMessageSourceAspectSpec extends Specification {

    MessageSource delegateMessageSource = Mock(MessageSource)
    ProceedingJoinPoint pjp = Mock(ProceedingJoinPoint)

    @Subject
    CxMessageSourceAspect aspect = new CxMessageSourceAspect(delegateMessageSource)

    def setup() {
        aspect.setEnabled(true)
    }

    // ---------- enabled=false ----------

    def "when aspect is disabled it proceeds with original join point"() {
        given:
        aspect.setEnabled(false)
        pjp.proceed() >> "original"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "original"
        0 * delegateMessageSource._
    }

    // ---------- target is CxDatabaseMessageSource ----------

    def "when target is CxDatabaseMessageSource it proceeds with original join point"() {
        given:
        def dbSource = Mock(CxDatabaseMessageSource)
        pjp.getTarget() >> dbSource
        pjp.proceed() >> "db-original"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "db-original"
        0 * delegateMessageSource._
    }

    // ---------- 2-arg variant: getMessage(MessageSourceResolvable, Locale) ----------

    def "delegates 2-arg getMessage(MessageSourceResolvable, Locale) to database message source"() {
        given:
        def resolvable = Mock(MessageSourceResolvable)
        def locale = Locale.ENGLISH
        pjp.getTarget() >> Mock(Object)
        pjp.getArgs() >> ([resolvable, locale] as Object[])
        delegateMessageSource.getMessage(resolvable, locale) >> "db-message"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "db-message"
        0 * pjp.proceed()
    }

    def "falls back to original join point when 2-arg getMessage throws NoSuchMessageException"() {
        given:
        def resolvable = Mock(MessageSourceResolvable)
        def locale = Locale.ENGLISH
        pjp.getTarget() >> Mock(Object)
        pjp.getArgs() >> ([resolvable, locale] as Object[])
        delegateMessageSource.getMessage(resolvable, locale) >> { throw new NoSuchMessageException("key") }
        pjp.proceed() >> "fallback"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "fallback"
    }

    // ---------- 3-arg variant: getMessage(String, Object[], Locale) ----------

    def "delegates 3-arg getMessage(String, Object[], Locale) to database message source"() {
        given:
        def code = "some.key"
        def args = ["arg1"] as Object[]
        def locale = Locale.GERMAN
        pjp.getTarget() >> Mock(Object)
        pjp.getArgs() >> ([code, args, locale] as Object[])
        delegateMessageSource.getMessage(code, args, locale) >> "db-message-3"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "db-message-3"
        0 * pjp.proceed()
    }

    def "falls back to original join point when 3-arg getMessage throws NoSuchMessageException"() {
        given:
        def code = "some.key"
        def args = [] as Object[]
        def locale = Locale.FRENCH
        pjp.getTarget() >> Mock(Object)
        pjp.getArgs() >> ([code, args, locale] as Object[])
        delegateMessageSource.getMessage(code, args, locale) >> { throw new NoSuchMessageException(code) }
        pjp.proceed() >> "fallback-3"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "fallback-3"
    }

    // ---------- 4-arg variant: getMessage(String, Object[], String, Locale) ----------

    def "delegates 4-arg getMessage(String, Object[], String, Locale) to database message source"() {
        given:
        def code = "some.key"
        def args = [] as Object[]
        def defaultMsg = "default"
        def locale = Locale.ITALIAN
        pjp.getTarget() >> Mock(Object)
        pjp.getArgs() >> ([code, args, defaultMsg, locale] as Object[])
        delegateMessageSource.getMessage(code, args, defaultMsg, locale) >> "db-message-4"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "db-message-4"
        0 * pjp.proceed()
    }

    def "4-arg getMessage falls back when database returns null"() {
        given:
        def code = "missing.key"
        def args = [] as Object[]
        def defaultMsg = "default"
        def locale = Locale.ITALIAN
        pjp.getTarget() >> Mock(Object)
        pjp.getArgs() >> ([code, args, defaultMsg, locale] as Object[])
        delegateMessageSource.getMessage(code, args, defaultMsg, locale) >> null
        pjp.proceed() >> "original-4"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "original-4"
    }

    // ---------- null returned from database falls back to original ----------

    def "falls back to original join point when database message source returns null for 2-arg variant"() {
        given:
        def resolvable = Mock(MessageSourceResolvable)
        def locale = Locale.ENGLISH
        pjp.getTarget() >> Mock(Object)
        pjp.getArgs() >> ([resolvable, locale] as Object[])
        delegateMessageSource.getMessage(resolvable, locale) >> null
        pjp.proceed() >> "fallback-null"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "fallback-null"
    }

    // ---------- recursion guard ----------

    def "recursion guard prevents re-entrant calls and proceeds with original"() {
        given: "simulate re-entrant call by setting the ThreadLocal ACTIVE flag"
        def activeField = CxMessageSourceAspect.getDeclaredField("ACTIVE")
        activeField.setAccessible(true)
        ThreadLocal<Boolean> active = (ThreadLocal<Boolean>) activeField.get(null)
        active.set(Boolean.TRUE)

        and:
        pjp.getTarget() >> Mock(Object)
        pjp.proceed() >> "recursion-guarded"

        when:
        def result = aspect.delegateToDatabaseMessageSource(pjp)

        then:
        result == "recursion-guarded"
        0 * delegateMessageSource._

        cleanup:
        active.set(Boolean.FALSE)
    }

    // ---------- reflection coverage check ----------

    def "all MessageSource interface methods are handled by the aspect"() {
        given: "collect all getMessage method signatures defined in MessageSource"
        final List<Method> messageSourceMethods = MessageSource.declaredMethods.findAll { it.name == "getMessage" }

        and: "the arg-count branches implemented in CxMessageSourceAspect"
        final Set<Integer> coveredArgCounts = [2, 3, 4] as Set

        expect: "every MessageSource#getMessage overload has a matching branch in the aspect"
        messageSourceMethods.each { final Method method ->
            final int argCount = method.parameterCount
            assert coveredArgCounts.contains(argCount),
                "MessageSource method '${method.toGenericString()}' with ${argCount} parameter(s) is NOT covered by CxMessageSourceAspect"
        }

        and: "no extra arg-count branches are claimed without a corresponding MessageSource method"
        final Set<Integer> actualArgCounts = messageSourceMethods.collect { it.parameterCount } as Set
        assert coveredArgCounts == actualArgCounts,
            "CxMessageSourceAspect claims to cover arg counts ${coveredArgCounts} but MessageSource only defines ${actualArgCounts}"
    }
}
