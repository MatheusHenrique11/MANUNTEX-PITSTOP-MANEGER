import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '@env/environment';
import { UserRequest, UserResponse } from '@core/models/user.model';

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/users`;

  listar() {
    return this.http.get<UserResponse[]>(this.url);
  }

  criar(req: UserRequest) {
    return this.http.post<UserResponse>(this.url, req);
  }

  alterarStatus(id: string, enabled: boolean) {
    return this.http.patch<UserResponse>(`${this.url}/${id}/status`, null, {
      params: { enabled: String(enabled) },
    });
  }

  alterarRole(id: string, req: Pick<UserRequest, 'role' | 'fullName' | 'password'>) {
    return this.http.patch<UserResponse>(`${this.url}/${id}/role`, req);
  }
}
