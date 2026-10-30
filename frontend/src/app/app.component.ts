import {Component, OnInit, OnDestroy, HostListener} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {IconComponent} from './icon.component';
import {DeskComponent} from './desk.component';
import {ApiService, ApiError} from './api.service';
import {Person, Ticket, Detail, Meta, Summary, Article, Notice, Status, STATUS, PRIORITY, ROLES, Item} from './models';
type View = 'dashboard'|'tickets'|'board'|'departments'|'knowledge'|'reports'|'users'|'settings';
@Component({selector:'app-root',standalone:true,imports:[CommonModule,FormsModule,IconComponent,DeskComponent],templateUrl:'./app.component.html'})
export class AppComponent implements OnInit,OnDestroy {
  user:Person|null=null; booting=true; loading=false; busy=false; demo=false;
  email=''; nome=''; senha=''; showPassword=false; loginError=''; view:View='dashboard'; mobileMenu=false; noticesOpen=false;
  atendimento=true; deskMode=false; legacyIntake=false; accountOpen=false; dragging=false; opening=false; formError=''; studioPane:'novo'|'meus'='novo'; myTickets:Ticket[]=[]; deskDetail:Detail|null=null; mineLoading=false; minePage=0; attachmentPreview='';
  theme=localStorage.getItem('cr-theme')||'light'; toast=''; toastError=false; private toastTimer:ReturnType<typeof setTimeout>|undefined; private poll:ReturnType<typeof setInterval>|undefined;
  statusLabels=STATUS; priorityLabels=PRIORITY; roles=ROLES; statuses=Object.keys(STATUS); priorities=Object.keys(PRIORITY);
  pages:Record<View,{title:string;subtitle:string;icon:string}>={
    dashboard:{title:'Visão geral',subtitle:'Um panorama do suporte. Tudo no seu controle.',icon:'dashboard'},
    tickets:{title:'Central de chamados',subtitle:'Cada solicitação, uma solução. Acompanhe de perto.',icon:'ticket'},
    board:{title:'Quadro de atendimento',subtitle:'Organize o fluxo de trabalho da equipe de TI.',icon:'board'},
    departments:{title:'Departamentos',subtitle:'Todos os setores conectados ao mesmo suporte.',icon:'department'},
    knowledge:{title:'Base de conhecimento',subtitle:'Respostas que ajudam. Conhecimento que simplifica.',icon:'book'},
    reports:{title:'Relatórios e indicadores',subtitle:'Transforme os dados do atendimento em boas decisões.',icon:'chart'},
    users:{title:'Pessoas e acessos',subtitle:'As pessoas certas, com as permissões certas.',icon:'users'},
    settings:{title:'Configurações',subtitle:'Sua conta, preferências e organização.',icon:'settings'}
  };
  meta:Meta={departments:[],categories:[],technicians:[],slaHours:{CRITICA:4,ALTA:8,MEDIA:24,BAIXA:72}};
  summary:Summary={total:0,active:0,overdue:0,resolved:0,slaPercent:100,averageResolutionHours:0,averageRating:0,daily:[],byStatus:[]};
  tickets:Ticket[]=[];recent:Ticket[]=[];total=0;page=0;pageCount=0;range='30';mine=false;
  filters={q:'',status:'',priority:'',department:'',category:'',overdue:''};
  departments:any[]=[];users:Person[]=[];articles:Article[]=[];notices:Notice[]=[];catalogs:{departments:Item[];categories:Item[]}={departments:[],categories:[]};
  userQuery='';articleQuery='';articleCategory='';activeArticle:Article|null=null;
  modal:''|'ticket'|'user'|'article'|'catalog'='';selected:Detail|null=null;detailTab:'conversation'|'history'='conversation';
  ticketForm={requesterName:'',title:'',description:'',categoryId:'',priority:'',location:'',equipment:'',contact:'',triageId:null as number|null};newFiles:File[]=[];
  patchForm={status:'',priority:'',assigneeId:'',solution:'',reason:''};commentText='';internalNote=false;
  userForm:any={};articleForm:any={};catalogForm:any={};catalogType:'departments'|'categories'='departments';
  account={currentPassword:'',newPassword:'',confirm:''};
  boardColumns=[{id:'ABERTO',title:'Na fila',dot:'blue'},{id:'EM_ANDAMENTO',title:'Em atendimento',dot:'yellow'},{id:'AGUARDANDO_USUARIO',title:'Aguardando',dot:'purple'},{id:'RESOLVIDO',title:'Resolvidos',dot:'green'}];
  constructor(public api:ApiService){}
  get staff():boolean{return !!this.user&&this.user.role!=='COLABORADOR';}
  get admin():boolean{return this.user?.role==='ADMIN';}
  get firstName():string{return this.user?.name.split(' ')[0]||'';}
  get mineView():Ticket[]{const start=this.minePage*9;return this.myTickets.slice(start,start+9);}
  get minePageCount():number{return Math.max(1,Math.ceil(this.myTickets.length/9));}
  get unread():number{return this.notices.filter(n=>!n.read).length;}
  get today():string{return new Date().toLocaleDateString('pt-BR',{day:'2-digit',month:'short',year:'numeric'});}
  get heading():{title:string;subtitle:string;icon:string}{return this.pages[this.view];}
  get filteredUsers():Person[]{const q=this.userQuery.toLocaleLowerCase();return this.users.filter(u=>(u.name+' '+u.email+' '+u.department).toLocaleLowerCase().includes(q));}
  get filteredArticles():Article[]{const q=this.articleQuery.toLocaleLowerCase();return this.articles.filter(a=>(!this.articleCategory||a.category===this.articleCategory)&&(a.title+' '+a.excerpt+' '+a.body).toLocaleLowerCase().includes(q));}
  get articleCategories():string[]{return [...new Set(this.articles.map(a=>a.category))];}
  get donutStyle():string {let start=0;const colors:Record<string,string>={ABERTO:'#517baf',EM_ANDAMENTO:'#e6b316',AGUARDANDO_USUARIO:'#a48acb',AGUARDANDO_TERCEIROS:'#c389a9',RESOLVIDO:'#4ba886',FECHADO:'#42705f',CANCELADO:'#a8b3bd'};if(!this.summary.total)return 'var(--line)';const stops=this.summary.byStatus.filter(x=>x.count>0).map(x=>{const end=start+x.count/this.summary.total*100;const stop=colors[x.status]+' '+start+'% '+end+'%';start=end;return stop;});return 'conic-gradient('+stops.join(',')+')';}
  @HostListener('document:keydown',['$event']) trapFocus(event:KeyboardEvent):void {if(event.key!=='Tab'||(!this.modal&&!this.selected&&!this.activeArticle))return;const dialogs=document.querySelectorAll<HTMLElement>('[role="dialog"]');const dialog=dialogs[dialogs.length-1];if(!dialog)return;const fields=Array.from(dialog.querySelectorAll<HTMLElement>('button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),a[href]')).filter(e=>e.offsetParent!==null);if(!fields.length)return;const first=fields[0],last=fields[fields.length-1];if(event.shiftKey&&(document.activeElement===first||!dialog.contains(document.activeElement))){event.preventDefault();last.focus();}else if(!event.shiftKey&&(document.activeElement===last||!dialog.contains(document.activeElement))){event.preventDefault();first.focus();}}
  get chartMax():number{return Math.max(1,...this.summary.daily.flatMap(d=>[d.created,d.resolved]));}
  get needsReason():boolean{return this.patchForm.status==='CANCELADO'||!!this.selected&&['RESOLVIDO','FECHADO'].includes(this.selected.ticket.status)&&this.patchForm.status==='EM_ANDAMENTO';}
  async ngOnInit():Promise<void>{
    document.documentElement.dataset['theme']=this.theme;
    try{const info=await this.api.request<{demo:boolean}>('/public/info');this.demo=info.demo;await this.api.csrf();this.user=await this.api.request<Person>('/auth/me');await this.startWorkspace();}
    catch(e){if(!(e instanceof ApiError&&e.status===401))this.loginError=this.errorText(e);}
    finally{this.booting=false;}
    this.poll=setInterval(()=>{if(this.user&&!this.user.mustChangePassword)void this.refreshNotifications();},30000);
  }
  ngOnDestroy():void{if(this.poll)clearInterval(this.poll);if(this.toastTimer)clearTimeout(this.toastTimer);}
  @HostListener('window:keydown.escape') escape():void{if(!this.busy){this.modal='';this.selected=null;this.activeArticle=null;}this.noticesOpen=false;this.mobileMenu=false;}
  @HostListener('window:hashchange') async hashChange():Promise<void>{if(!this.user||this.deskMode||!this.atendimento||!this.legacyIntake)return;const pane=location.hash.slice(1).split('/')[0];if(pane==='meus'){if(this.studioPane!=='meus')await this.showMine();}else if(pane==='novo'){if(this.studioPane!=='novo'||this.deskDetail){this.studioPane='novo';this.deskDetail=null;}}else location.hash=this.studioPane==='meus'?'meus':'novo';}
  errorText(e:unknown):string{return e instanceof Error?e.message:'Não foi possível concluir. Tente novamente.';}
  notice(message:string,error=false):void{this.toast=message;this.toastError=error;if(this.toastTimer)clearTimeout(this.toastTimer);this.toastTimer=setTimeout(()=>this.toast='',6500);}
  handleError(e:unknown):void{if(e instanceof ApiError&&e.status===401){this.user=null;this.selected=null;this.modal='';}this.notice(this.errorText(e),true);}
  async action(task:()=>Promise<void>):Promise<void>{if(this.busy)return;this.busy=true;try{await task();}catch(e){this.handleError(e);}finally{this.busy=false;}}
  async login():Promise<void>{if(this.busy)return;const nome=this.nome.trim().replace(/\s+/g,' ').split(' ')[0]||'';if(!nome||!this.senha){this.loginError='Informe o primeiro nome e a senha.';return;}this.busy=true;this.loginError='';try{await this.api.loginByName(nome,this.senha);this.user=await this.api.request<Person>('/auth/me');this.senha='';await this.startWorkspace();}catch(e){this.loginError=this.errorText(e);}finally{this.busy=false;}}
  async logout():Promise<void>{await this.action(async()=>{try{await this.api.logout();}catch{/* a sessão pode já ter sido encerrada */}this.user=null;this.selected=null;this.modal='';this.notices=[];this.senha='';this.atendimento=true;this.deskMode=false;this.accountOpen=false;this.studioPane='novo';this.deskDetail=null;this.myTickets=[];this.minePage=0;this.formError='';this.clearAttachment();location.hash='';});}
  async startWorkspace():Promise<void>{this.atendimento=true;this.accountOpen=false;if(this.staff){this.meta=await this.api.request<Meta>('/meta');this.deskMode=true;return;}if(!this.legacyIntake){this.deskMode=false;return;}this.meta=await this.api.request<Meta>('/meta');const pane=location.hash.slice(1).split('/')[0];if(pane==='meus')await this.showMine();else this.focusForm();}
  async navigate(view:View,update=true):Promise<void>{if(this.user?.mustChangePassword&&view!=='settings'){this.notice('Altere sua senha temporária antes de continuar.',true);return;}if(['users'].includes(view)&&!this.admin)return;if(['board','departments','reports'].includes(view)&&!this.staff)return;if(view!==this.view&&['tickets','board'].includes(view))this.page=0;this.view=view;this.mobileMenu=false;this.noticesOpen=false;if(update)location.hash=view;await this.loadView();}
  async loadView():Promise<void>{this.loading=true;try{switch(this.view){case 'dashboard':await this.refreshDashboard();break;case 'tickets':case 'board':await this.loadTickets();break;case 'departments':this.departments=await this.api.request<any[]>('/departments/overview');break;case 'knowledge':this.articles=await this.api.request<Article[]>('/articles');break;case 'reports':await this.refreshDashboard();this.departments=await this.api.request<any[]>('/departments/overview');break;case 'users':this.users=await this.api.request<Person[]>('/admin/users');break;case 'settings':if(this.admin&&!this.user?.mustChangePassword)this.catalogs=await this.api.request('/admin/catalogs');break;default:break;}}catch(e){this.handleError(e);}finally{this.loading=false;}}
  async refreshDashboard():Promise<void>{const [s,t]=await Promise.all([this.api.request<Summary>('/dashboard?days='+this.range),this.api.request<{items:Ticket[]}>('/tickets?size=5')]);this.summary=s;this.recent=t.items;}
  async rangeChange():Promise<void>{try{await this.refreshDashboard();}catch(e){this.handleError(e);}}
  async refreshNotifications():Promise<void>{try{this.notices=await this.api.request<Notice[]>('/notifications');}catch(e){if(e instanceof ApiError&&e.status===401)this.user=null;}}
  query(extra:Record<string,string>={}):string{const q=new URLSearchParams();for(const [k,v] of Object.entries({...this.filters,mine:String(this.mine),...extra}))if(v)q.set(k,v);return q.toString();}
  async loadTickets():Promise<void>{const size=this.view==='board'?200:12;const result=await this.api.request<{items:Ticket[];total:number;pages:number}>('/tickets?'+this.query({page:String(this.page),size:String(size)}));this.tickets=result.items;this.total=result.total;this.pageCount=result.pages;}
  async applyFilters():Promise<void>{this.page=0;this.loading=true;try{await this.loadTickets();}catch(e){this.handleError(e);}finally{this.loading=false;}}
  async clearFilters():Promise<void>{this.filters={q:'',status:'',priority:'',department:'',category:'',overdue:''};this.mine=false;await this.applyFilters();}
  async goPage(offset:number):Promise<void>{this.page+=offset;await this.loadView();}
  async searchGlobal():Promise<void>{this.page=0;await this.navigate('tickets');}
  async overdueTickets():Promise<void>{this.filters.overdue='true';this.page=0;await this.navigate('tickets');}
  initials(name:string):string{return name.split(' ').filter(Boolean).slice(0,2).map(n=>n[0]).join('').toUpperCase();}
  terminal(t:Ticket):boolean{return ['RESOLVIDO','FECHADO','CANCELADO'].includes(t.status);}
  relative(value:string):string{const minutes=Math.max(0,Math.floor((Date.now()-Date.parse(value))/60000));if(minutes<1)return 'Agora';if(minutes<60)return 'Há '+minutes+' min';const hours=Math.floor(minutes/60);return hours<24?'Há '+hours+' h':'Há '+Math.floor(hours/24)+' dias';}
  deadline(t:Ticket):string{if(this.terminal(t))return 'Finalizado';const minutes=Math.floor((Date.parse(t.dueAt)-Date.now())/60000);const hours=Math.abs(Math.round(minutes/60));return minutes<0?'Atrasado '+hours+' h':minutes<60?'Restam '+minutes+' min':'Restam '+hours+' h';}
  statusNote(status:string):string{return ({ABERTO:'Seu chamado foi registrado e está aguardando atendimento pela equipe de TI.',EM_ANDAMENTO:'A equipe de TI está trabalhando na sua solicitação.',AGUARDANDO_USUARIO:'A equipe de TI aguarda uma informação para continuar o atendimento.',AGUARDANDO_TERCEIROS:'A equipe de TI está tratando a solicitação com um terceiro.',RESOLVIDO:'Seu chamado foi resolvido pela equipe de TI.',FECHADO:'Este chamado foi encerrado.',CANCELADO:'Este chamado foi cancelado.'} as Record<string,string>)[status]||'Acompanhe o andamento desta solicitação.';}
  statusIcon(status:string):string{if(status==='RESOLVIDO'||status==='FECHADO')return 'checkCircle';if(status==='CANCELADO')return 'close';if(status==='EM_ANDAMENTO'||status.startsWith('AGUARDANDO'))return 'clock';return 'info';}
  dayLabel(date:string):string{return new Date(date+'T12:00:00').toLocaleDateString('pt-BR',{weekday:'short'}).replace('.','');}
  pointPath(key:'created'|'resolved'):string{return this.summary.daily.map((d,i)=>`${i?'L':'M'} ${35+i*92} ${166-(d[key]/this.chartMax)*130}`).join(' ');}
  areaPath():string{return this.pointPath('created')+' L 587 180 L 35 180 Z';}
  boardItems(column:string):Ticket[]{return this.tickets.filter(t=>column==='AGUARDANDO_USUARIO'?t.status.startsWith('AGUARDANDO'):column==='RESOLVIDO'?['RESOLVIDO','FECHADO'].includes(t.status):t.status===column);}
  dragTicket(event:DragEvent,t:Ticket):void{event.dataTransfer?.setData('text/plain',String(t.id));}
  allowDrop(event:DragEvent):void{event.preventDefault();}
  async dropTicket(event:DragEvent,status:string):Promise<void>{event.preventDefault();const id=Number(event.dataTransfer?.getData('text/plain'));if(!id)return;await this.openTicket(id);if(this.selected?.transitions.includes(status as Status)){this.patchForm.status=status;this.notice('Confirme a alteração de status no painel do chamado.');}else this.notice('Essa transição não é permitida a partir do status atual.',true);}
  blankTicket(){return {requesterName:'',title:'',description:'',categoryId:'',priority:'',location:'',equipment:'',contact:'',triageId:null as number|null};}
  newTicket():void{this.ticketForm=this.atendimento?this.blankTicket():{...this.blankTicket(),priority:'MEDIA'};this.clearAttachment();this.formError='';if(this.atendimento){this.modal='';this.studioPane='novo';this.deskDetail=null;setTimeout(()=>document.getElementById('solicitante')?.focus(),0);return;}this.modal='ticket';}
  private takeImage(file:File):boolean{const ext=file.name.split('.').pop()?.toLowerCase()||'';if(!['png','jpg','jpeg'].includes(ext)||file.size<1||file.size>5*1024*1024){this.notice('Use uma imagem PNG, JPG ou JPEG de até 5 MB.',true);return false;}this.newFiles=[file];const reader=new FileReader();reader.onload=()=>{this.attachmentPreview=String(reader.result||'');};reader.readAsDataURL(file);return true;}
  clearAttachment():void{this.newFiles=[];this.attachmentPreview='';const input=document.getElementById('anexo') as HTMLInputElement|null;if(input)input.value='';}
  fileSelection(event:Event):void{const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file||!this.takeImage(file)){input.value='';if(!this.newFiles.length)this.attachmentPreview='';}}
  dropFiles(event:DragEvent):void{event.preventDefault();this.dragging=false;const file=event.dataTransfer?.files?.[0];if(file)this.takeImage(file);}
  async submitTicket():Promise<void>{if(this.opening||this.busy)return;const name=this.ticketForm.requesterName.trim();if(name.length<3||this.ticketForm.title.trim().length<5||!this.ticketForm.categoryId||!this.ticketForm.priority||this.ticketForm.description.trim().length<15){this.formError='Preencha o nome completo, o assunto, a categoria, a prioridade e uma descrição com pelo menos 15 caracteres.';this.notice(this.formError,true);return;}this.formError='';this.opening=true;try{await this.action(async()=>{const created=await this.api.request<Ticket>('/tickets','POST',{...this.ticketForm,requesterName:name,categoryId:Number(this.ticketForm.categoryId),triageId:null});this.modal='';let failures=0;for(const file of this.newFiles){const body=new FormData();body.append('file',file);try{await this.api.request('/tickets/'+created.id+'/attachments','POST',body);}catch{failures++;}}this.clearAttachment();const message=failures?`${created.protocol} aberto, mas a imagem não foi enviada. Tente anexá-la novamente.`:`${created.protocol} aberto com sucesso.`;if(this.atendimento){this.ticketForm=this.blankTicket();if(this.studioPane==='meus')await this.reloadMine();this.notice(message,failures>0);return;}await this.refreshDashboard();await this.openTicket(created.id);if(this.view==='tickets'||this.view==='board')await this.loadTickets();this.notice(message,failures>0);});}finally{this.opening=false;}}
  async openTicket(id:number):Promise<void>{try{this.selected=await this.api.request<Detail>('/tickets/'+id);this.setPatch();this.commentText='';this.internalNote=false;this.detailTab='conversation';}catch(e){this.handleError(e);}}
  setPatch():void{if(!this.selected)return;const t=this.selected.ticket;this.patchForm={status:t.status,priority:t.priority,assigneeId:t.assigneeId?String(t.assigneeId):'',solution:'',reason:''};}
  async saveTicket():Promise<void>{if(!this.selected)return;await this.action(async()=>{const t=this.selected!.ticket;const body:any={version:t.version};if(this.patchForm.status!==t.status)body.status=this.patchForm.status;if(this.staff){if(this.patchForm.priority!==t.priority)body.priority=this.patchForm.priority;if(this.patchForm.assigneeId!==(t.assigneeId?String(t.assigneeId):'')){if(this.patchForm.assigneeId)body.assigneeId=Number(this.patchForm.assigneeId);else body.unassign=true;}}if(this.patchForm.solution)body.solution=this.patchForm.solution;if(this.patchForm.reason)body.reason=this.patchForm.reason;await this.api.request('/tickets/'+t.id,'PATCH',body);await this.openTicket(t.id);await this.refreshDashboard();if(['tickets','board'].includes(this.view))await this.loadTickets();this.notice('Chamado atualizado com sucesso.');});}
  async takeTicket():Promise<void>{if(!this.selected||!this.user)return;this.patchForm.assigneeId=String(this.user.id);if(this.selected.ticket.status==='ABERTO')this.patchForm.status='EM_ANDAMENTO';await this.saveTicket();}
  async sendComment():Promise<void>{if(!this.selected||!this.commentText.trim())return;await this.action(async()=>{const id=this.selected!.ticket.id;await this.api.request('/tickets/'+id+'/comments','POST',{body:this.commentText,internal:this.internalNote});this.selected=await this.api.request<Detail>('/tickets/'+id);this.setPatch();this.commentText='';this.notice(this.internalNote?'Nota interna registrada.':'Mensagem enviada.');});}
  async attachToTicket(event:Event):Promise<void>{const input=event.target as HTMLInputElement;const file=input.files?.[0];if(!file||!this.selected)return;await this.action(async()=>{const body=new FormData();body.append('file',file);const id=this.selected!.ticket.id;await this.api.request('/tickets/'+id+'/attachments','POST',body);this.selected=await this.api.request<Detail>('/tickets/'+id);this.notice('Arquivo anexado.');});input.value='';}
  async downloadAttachment(id:number,name:string):Promise<void>{await this.action(async()=>{await this.api.download('/attachments/'+id,name);});}
  async rate(value:number):Promise<void>{if(!this.selected)return;await this.action(async()=>{const id=this.selected!.ticket.id;await this.api.request('/tickets/'+id+'/rating','POST',{rating:value});await this.openTicket(id);this.notice('Obrigada pela sua avaliação!');});}
  async markAll():Promise<void>{await this.action(async()=>{await this.api.request('/notifications/read-all','POST');await this.refreshNotifications();});}
  async openNotice(n:Notice):Promise<void>{try{await this.api.request('/notifications/'+n.id+'/read','POST');n.read=true;this.noticesOpen=false;if(n.ticketId)await this.openTicket(n.ticketId);}catch(e){this.handleError(e);}}
  async exportReport():Promise<void>{await this.action(async()=>{await this.api.download('/reports/export?days='+this.range,'Chamado-Rapido-relatorio.csv');this.notice('Relatório exportado.');});}
  editUser(u?:Person):void{this.userForm=u?{...u,password:''}:{name:'',email:'',role:'COLABORADOR',departmentId:'',password:'',active:true};this.modal='user';}
  async saveUser():Promise<void>{await this.action(async()=>{const u=this.userForm;await this.api.request('/admin/users'+(u.id?'/'+u.id:''),u.id?'PUT':'POST',{...u,departmentId:Number(u.departmentId)});this.modal='';this.users=await this.api.request<Person[]>('/admin/users');this.meta=await this.api.request<Meta>('/meta');this.notice('Usuário salvo. Senhas temporárias devem ser trocadas no primeiro acesso.');});}
  editArticle(a?:Article):void{this.articleForm=a?{...a}:{title:'',excerpt:'',body:'',category:'Boas práticas',active:true};this.activeArticle=null;this.modal='article';}
  async saveArticle():Promise<void>{await this.action(async()=>{const a=this.articleForm;await this.api.request('/admin/articles'+(a.id?'/'+a.id:''),a.id?'PUT':'POST',a);this.modal='';this.articles=await this.api.request<Article[]>('/articles');this.notice('Artigo salvo na base de conhecimento.');});}
  editCatalog(type:'departments'|'categories',item?:Item):void{this.catalogType=type;this.catalogForm=item?{...item}:{name:'',description:'',active:true};this.modal='catalog';}
  async saveCatalog():Promise<void>{await this.action(async()=>{const c=this.catalogForm;await this.api.request('/admin/'+this.catalogType+(c.id?'/'+c.id:''),c.id?'PUT':'POST',c);this.modal='';this.catalogs=await this.api.request('/admin/catalogs');this.meta=await this.api.request<Meta>('/meta');if(this.view==='departments')this.departments=await this.api.request<any[]>('/departments/overview');this.notice('Cadastro atualizado.');});}
  toggleTheme():void{this.theme=this.theme==='light'?'dark':'light';localStorage.setItem('cr-theme',this.theme);document.documentElement.dataset['theme']=this.theme;}
  async changePassword():Promise<void>{if(this.account.newPassword!==this.account.confirm){this.notice('A confirmação de senha não confere.',true);return;}await this.action(async()=>{await this.api.request('/auth/password','POST',this.account);this.account={currentPassword:'',newPassword:'',confirm:''};this.user=null;this.notice('Senha alterada. Entre novamente com a nova senha.');});}
  openPainel():void{this.atendimento=false;this.accountOpen=false;void this.navigate('dashboard');}
  voltarAtendimento():void{this.atendimento=true;this.accountOpen=false;}
  focusForm():void{this.studioPane='novo';this.deskDetail=null;if(location.hash!=='#novo')location.hash='novo';setTimeout(()=>document.getElementById('solicitante')?.focus(),0);}
  async showMine():Promise<void>{this.studioPane='meus';this.deskDetail=null;if(location.hash!=='#meus')location.hash='meus';await this.reloadMine();}
  async reloadMine():Promise<void>{this.mineLoading=true;this.minePage=0;try{const result=await this.api.request<{items:Ticket[]}>('/tickets?size=100');this.myTickets=this.staff&&this.user?result.items.filter(t=>t.requesterId===this.user!.id):result.items;}catch(e){this.handleError(e);}finally{this.mineLoading=false;}}
  async openMine(id:number):Promise<void>{this.mineLoading=true;try{this.deskDetail=await this.api.request<Detail>('/tickets/'+id);this.commentText='';}catch(e){this.handleError(e);}finally{this.mineLoading=false;}}
}
