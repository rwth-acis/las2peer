export function request<TResponse extends RequestResponse>(
  url: string,
  config: RequestInit = {}
): Promise<TResponse> {
  let status: number;
  return fetch(url, config)
    .then((response) => {
      status = response.status;
      return response.text();
    })
    .then((text) => {
      try {
        const data = JSON.parse(text) as TResponse;
        data.code = status;
        return data;
      } catch (err) {
        const data: TResponse = Object();
        data.code = status;
        data.text = text;

        return data;
      }
    });
}
export function requestFile(
  url: string,
  config: RequestInit = {}
): Promise<FileResponse> {
  let status: number;
  return fetch(url, config)
    .then((response) => {
      status = response.status;
      return response.blob();
    })
    .then((blob) => {
      const data: FileResponse = { blob: blob, code: status };
      return data;
    });
}

export interface RequestResponse {
  code: number;
  text: string;
}

export interface FileResponse {
  code: number;
  blob: Blob;
}
