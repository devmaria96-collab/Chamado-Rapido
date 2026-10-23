import { bootstrapApplication } from '@angular/platform-browser';
import { provideZoneChangeDetection, LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { AppComponent } from './app/app.component';
registerLocaleData(localePt);
bootstrapApplication(AppComponent, { providers: [provideZoneChangeDetection({eventCoalescing: true}), {provide: LOCALE_ID, useValue: 'pt-BR'}] }).catch(console.error);
