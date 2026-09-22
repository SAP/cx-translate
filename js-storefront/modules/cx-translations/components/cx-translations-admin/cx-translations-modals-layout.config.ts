import { DIALOG_TYPE, LayoutConfig } from '@spartacus/storefront';
import { CxTranslationsUploadConfirmationDialogComponent } from './cx-translations-upload-confirmation-dialog/cx-translations-upload-confirmation-dialog.component';
import { CxTranslationsPublishingConfirmationDialogComponent } from './cx-translations-publishing-confirmation-dialog/cx-translations-publishing-confirmation-dialog.component';

export const CxTranslationsDialogModalLayoutConfig: LayoutConfig = {
  launch: {
    PUBLISH_TRANSLATIONS: {
      inline: true,
      component: CxTranslationsPublishingConfirmationDialogComponent,
      dialogType: DIALOG_TYPE.DIALOG,
    },
    UPLOAD_TRANSLATIONS: {
      inline: true,
      component: CxTranslationsUploadConfirmationDialogComponent,
      dialogType: DIALOG_TYPE.DIALOG,
    },
  },
};
