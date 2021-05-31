export function request<TResponse>(
  url: string,
  config: RequestInit = {}
): Promise<TResponse> {
  return fetch(url, config)
    .then((response) => {
      return response.json();
    })
    .then((data) => {
      return data as TResponse;
    });
}
