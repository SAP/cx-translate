/*
 * Copyright (c) 2026 SAP SE or an SAP affiliate company. All rights reserved.
 */
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from "@angular/common/http";
import { Injectable } from "@angular/core";
import {Observable} from "rxjs";
import {CxTranslationService} from "../service/cx-translation.service";

/**
 * Interceptor that removes Authorization header from CX translation requests for publicly accessible chunks.
 * This prevents errors in case of timeout or invalid/expired tokens that would break loading of translations.
 */
@Injectable({ providedIn: 'root' })
export class CxTranslationsAuthHttpInterceptor implements HttpInterceptor {
    intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
      if (this.isTranslationsRequest(req)) {
        const modifiedReq = req.clone({
          headers: req.headers.delete('Authorization'),
        });
        return next.handle(modifiedReq);
      }
      return next.handle(req)
    }

  protected isTranslationsRequest(request: HttpRequest<any>): boolean {
    return request.url.includes(CxTranslationService.ONLINE_TRANSLATIONS_PATH);
  }

}
