import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class SearchServiceMock {
  private httpClient = inject(HttpClient);


  /**
   * This mock is used to override some parameters to get json server working.
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  fetch(_page: number, _limit: number, _sort: string, q?: string): Observable<any> {
    const collected = { _page: _page + 1, _limit, _sort, _order: _sort.slice(_sort.indexOf(',') + 1) };
    const params = new HttpParams({ fromObject: q ? { ...collected, q } : collected });

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    return this.httpClient.get<any>(`${environment.api}getx`, { params }).pipe(
      map((result) => {
        const content = { content: result };
        const totalElements = q ? content.content.length : 25;
        return { ...content, totalElements };
      })
    );
  }
}
