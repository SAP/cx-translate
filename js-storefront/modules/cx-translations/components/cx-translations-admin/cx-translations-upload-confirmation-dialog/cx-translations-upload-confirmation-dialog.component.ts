/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */

import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  HostListener,
  NgZone,
} from '@angular/core';
import { GlobalMessageService, GlobalMessageType } from '@spartacus/core';
import { FocusConfig, LaunchDialogService } from '@spartacus/storefront';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { CxTranslationService } from '../../../service/cx-translation.service';

@Component({
  selector: 'app-cx-translations-upload-confirmation-dialog ',
  templateUrl: './cx-translations-upload-confirmation-dialog.component.html',
  styleUrls: ['./cx-translations-upload-confirmation-dialog.component.scss'],
  standalone: false,
})
export class CxTranslationsUploadConfirmationDialogComponent {
  isUploading = false;

  focusConfig: FocusConfig = {
    trap: true,
    block: true,
    autofocus: 'button',
    focusOnEscape: true,
  };

  @HostListener('click', ['$event'])
  handleClick(event: UIEvent): void {
    if (event.target === this.el.nativeElement) {
      this.dismissModal('Cross click');
    }
  }

  constructor(
    protected globalMessageService: GlobalMessageService,
    protected launchDialogService: LaunchDialogService,
    protected el: ElementRef,
    protected cxTranslationsService: CxTranslationService,
    protected cdr: ChangeDetectorRef,
    protected ngZone: NgZone
  ) {}

  onConfirm(): void {
    this.isUploading = true; // Show spinner
    // The dialog is rendered inline inside an OnPush host, so async state
    // changes here won't be picked up automatically. Trigger detection
    // explicitly so the spinner actually shows.
    this.cdr.detectChanges();

    this.cxTranslationsService
      .uploadTranslations()
      .pipe(
        catchError((error) => {
          this.ngZone.run(() => {
            this.isUploading = false; // hide spinner
            this.dismissModal('Error');
            this.globalMessageService.add(
              {
                key: 'translations.upload.failed',
              },
              GlobalMessageType.MSG_TYPE_ERROR
            );
          });
          return throwError(() => error);
        })
      )
      .subscribe(() => {
        this.ngZone.run(() => {
          this.isUploading = false; // hide spinner
          this.dismissModal('Success');
          this.globalMessageService.add(
            {
              key: 'translations.upload.success',
            },
            GlobalMessageType.MSG_TYPE_CONFIRMATION
          );
        });
      });
  }

  dismissModal(reason?: any): void {
    this.isUploading = false;
    this.cdr.detectChanges();
    this.launchDialogService.closeDialog(reason);
  }
}
