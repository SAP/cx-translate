/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import {NgModule} from '@angular/core';
import {AsmModule} from '@spartacus/asm';
import {CsAgentAuthService} from "@spartacus/asm/root";
import {CxTranslationsModule} from '../cx-translations/cx-translations.module'
import {USER_REFRESH_SOURCES} from '../cx-translations/service/cx-translation.service'

// Angular module that provides support for ASM in CX Translations.
// It triggers refresh of Translations Admin UI when ASM agent logs in or out.
@NgModule({
  declarations: [

  ],
  imports: [
    AsmModule,
    CxTranslationsModule
  ],
  providers: [
     {
      provide: USER_REFRESH_SOURCES,
      multi: true,
      deps: [CsAgentAuthService],
      useFactory: (auth: CsAgentAuthService) => {
        return auth.isCustomerSupportAgentLoggedIn();
      }
    },
  ],
})
export class CxTranslationsASMModule {}
