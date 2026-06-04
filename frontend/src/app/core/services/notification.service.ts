import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  NotificationConfig,
  NotificationConfigRequest,
  NotificationLog,
  NotificationTemplate,
  NotificationTemplateUpdateRequest,
  PageResponse,
} from '@core/models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/notifications`;

  // ── Templates ───────────────────────────────────────────────────────────────

  getTemplates(): Observable<NotificationTemplate[]> {
    return this.http.get<NotificationTemplate[]>(`${this.base}/templates`);
  }

  updateTemplate(id: string, body: NotificationTemplateUpdateRequest): Observable<NotificationTemplate> {
    return this.http.put<NotificationTemplate>(`${this.base}/templates/${id}`, body);
  }

  seedTemplates(): Observable<void> {
    return this.http.post<void>(`${this.base}/templates/seed`, {});
  }

  // ── Configuração ─────────────────────────────────────────────────────────────

  getConfig(): Observable<NotificationConfig> {
    return this.http.get<NotificationConfig>(`${this.base}/config`);
  }

  saveConfig(body: NotificationConfigRequest): Observable<NotificationConfig> {
    return this.http.put<NotificationConfig>(`${this.base}/config`, body);
  }

  // ── Logs ─────────────────────────────────────────────────────────────────────

  getLogs(page = 0, size = 20): Observable<PageResponse<NotificationLog>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<NotificationLog>>(`${this.base}/logs`, { params });
  }
}
