import {Injectable} from '@angular/core';

export type DeskTheme = 'dark' | 'light';

@Injectable({providedIn: 'root'})
export class ThemeService {
  theme: DeskTheme = localStorage.getItem('cr-theme') === 'light' ? 'light' : 'dark';

  toggle(): void {
    this.theme = this.theme === 'dark' ? 'light' : 'dark';
    localStorage.setItem('cr-theme', this.theme);
  }
}
