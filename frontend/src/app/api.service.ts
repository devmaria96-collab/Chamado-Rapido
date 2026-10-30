import {Injectable} from '@angular/core';
export class ApiError extends Error {constructor(message:string,public status:number){super(message);}}
@Injectable({providedIn:'root'})
export class ApiService {
  private csrfToken=''; private csrfHeader='X-CSRF-TOKEN';
  async csrf():Promise<void>{const response=await fetch('/api/auth/csrf',{credentials:'same-origin',cache:'no-store'});if(!response.ok)throw new ApiError('Não foi possível iniciar uma sessão segura.',response.status);const data=await response.json();this.csrfToken=data.token;this.csrfHeader=data.headerName;}
  async request<T>(path:string,method='GET',body?:unknown):Promise<T>{
    if(method!=='GET'&&!this.csrfToken)await this.csrf();
    const headers:Record<string,string>={};if(method!=='GET')headers[this.csrfHeader]=this.csrfToken;
    let payload:BodyInit|undefined;
    if(body instanceof FormData||body instanceof URLSearchParams){payload=body;}else if(body!==undefined){headers['Content-Type']='application/json';payload=JSON.stringify(body);}
    let response:Response;
    try{response=await fetch('/api'+path,{method,headers,body:payload,credentials:'same-origin',cache:'no-store'});}catch{throw new ApiError('Servidor indisponível. Verifique a conexão e tente novamente.',0);}
    const content=await response.json().catch(()=>({}));
    if(!response.ok)throw new ApiError(content.message||'Não foi possível concluir a operação.',response.status);
    return content as T;
  }
  async login(email:string,password:string):Promise<void>{await this.csrf();await this.request('/auth/login','POST',new URLSearchParams({email,password}));await this.csrf();}
  async loginByName(nome:string,senha:string):Promise<void>{await this.csrf();await this.request('/auth/login','POST',new URLSearchParams({nome,password:senha}));await this.csrf();}
  async logout():Promise<void>{await this.request('/auth/logout','POST');this.csrfToken='';}
  async download(path:string,filename:string):Promise<void>{const r=await fetch('/api'+path,{credentials:'same-origin'});if(!r.ok)throw new ApiError('Não foi possível baixar o arquivo.',r.status);const blob=await r.blob();const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=filename;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
}
