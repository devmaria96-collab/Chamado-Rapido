export type Role = 'ADMIN'|'TECNICO'|'COLABORADOR';
export type Status = 'ABERTO'|'EM_ANDAMENTO'|'AGUARDANDO_USUARIO'|'AGUARDANDO_TERCEIROS'|'RESOLVIDO'|'FECHADO'|'CANCELADO';
export type Priority = 'BAIXA'|'MEDIA'|'ALTA'|'CRITICA';
export interface Person {id:number; name:string; email:string; role:Role; departmentId:number; department:string; active:boolean; mustChangePassword:boolean;}
export interface Ticket {id:number; version:number; protocol:string; title:string; description:string; requesterId:number; requester:string; departmentId:number; department:string; categoryId:number; category:string; assigneeId:number|null; assignee:string|null; status:Status; priority:Priority; location:string; equipment:string; contact:string; solution:string|null; rating:number|null; createdAt:string; updatedAt:string; dueAt:string; resolvedAt:string|null; closedAt:string|null; overdue:boolean;}
export interface Detail {ticket:Ticket; comments:{id:number;author:string;role:Role;body:string;internal:boolean;createdAt:string}[];history:{id:number;actor:string;action:string;details:string;createdAt:string}[];attachments:{id:number;name:string;mime:string;size:number;author:string;createdAt:string}[];transitions:Status[];}
export interface Item {id:number; name:string; active?:boolean; description?:string;}
export interface Meta {departments:Item[];categories:Item[];technicians:Item[];slaHours:Record<string,number>;}
export interface Summary {total:number;active:number;overdue:number;resolved:number;slaPercent:number;averageResolutionHours:number;averageRating:number;daily:{date:string;created:number;resolved:number}[];byStatus:{status:Status;count:number}[];}
export interface Article {id:number; title:string;excerpt:string;body:string;category:string;active:boolean;updatedAt:string;}
export interface Notice {id:number;title:string;read:boolean;ticketId:number;createdAt:string;}
export interface Triage {id:number;version:number;category:string;result:string;message:string;options:{id:string;label:string}[];transcript:string;ticketId:number|null;}
export const STATUS:Record<string,string>={ABERTO:'Aberto',EM_ANDAMENTO:'Em andamento',AGUARDANDO_USUARIO:'Aguardando usuário',AGUARDANDO_TERCEIROS:'Aguardando terceiros',RESOLVIDO:'Resolvido',FECHADO:'Encerrado',CANCELADO:'Cancelado'};
export const PRIORITY:Record<string,string>={BAIXA:'Baixa',MEDIA:'Média',ALTA:'Alta',CRITICA:'Crítica'};
export const ROLES:Record<string,string>={ADMIN:'Administrador',TECNICO:'Técnico de TI',COLABORADOR:'Colaborador'};
