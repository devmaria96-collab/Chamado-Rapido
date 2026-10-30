import {Component, EventEmitter, HostListener, Input, OnDestroy, OnInit, Output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {CdkDrag, CdkDragDrop, CdkDropList} from '@angular/cdk/drag-drop';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {IconComponent} from './icon.component';
import {ApiService, ApiError} from './api.service';
import {ThemeService} from './theme.service';
import {Detail, Item, Meta, Person, Status, Summary, Ticket, PRIORITY, ROLES, STATUS} from './models';

type DeskPage = 'painel'|'conversas'|'historico'|'equipe'|'configuracoes'|'perfil';
type ColumnId = 'novos'|'atendimento'|'aguardando'|'resolvidos';

@Component({
  selector: 'app-desk',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent, CdkDropList, CdkDrag],
  templateUrl: './desk.component.html'
})
export class DeskComponent implements OnInit, OnDestroy {
  @Input({required: true}) user!: Person;
  @Output() leave = new EventEmitter<void>();
  page: DeskPage = 'painel';
  collapsed = localStorage.getItem('cr-nav') === '1';
  counts: Record<string, number> | null = null;
  navOpen = false;
  profileOpen = false;
  conversationQuery = '';
  boardQuery = '';
  headerMenu = false;
  activeThreads = true;
  historyPriority = '';
  private seen: Record<string, string> = {};
  loading = true;
  busy = false;
  error = '';
  notice = '';
  whatsappNotice = 'O WhatsApp não está configurado. As mensagens do painel ficam no chamado e não são enviadas.';
  tickets: Ticket[] = [];
  detail: Detail | null = null;
  solution = '';
  reason = '';
  finishAsk = false;
  reopenAsk = false;
  transferTo = '';
  meta: Meta = {departments: [], categories: [], technicians: [], slaHours: {}};
  history: Ticket[] = [];
  historyTotal = 0;
  historyPages = 0;
  historyPage = 0;
  historyQuery = '';
  historyStatus = '';
  historyDepartment = '';
  historyAssignee = '';
  users: Person[] = [];
  departments: Item[] = [];
  summary: Summary | null = null;
  newUser = {name: '', email: '', role: 'TECNICO', departmentId: '', password: ''};
  newDepartment = '';
  account = {currentPassword: '', newPassword: '', confirm: ''};
  enlarged: Detail['attachments'][number] | null = null;
  brokenFiles: Record<number, boolean> = {};
  statusKeys = Object.keys(STATUS);
  priorityKeys = Object.keys(PRIORITY);
  statusLabels = STATUS;
  priorityLabels = PRIORITY;
  roles = ROLES;
  readonly dropIds: ColumnId[] = ['novos', 'atendimento', 'aguardando', 'resolvidos'];
  readonly indicators: {id: string; label: string; icon: string; tone: string}[] = [
    {id: 'novos', label: 'Novos', icon: 'file', tone: 'novos'},
    {id: 'atendimento', label: 'Em andamento', icon: 'settings', tone: 'atendimento'},
    {id: 'aguardando', label: 'Aguardando', icon: 'clock', tone: 'aguardando'},
    {id: 'resolvidosHoje', label: 'Resolvidos hoje', icon: 'checkCircle', tone: 'hoje'}
  ];
  columns: {id: ColumnId; title: string; hint: string; icon: string; statuses: Status[]; items: Ticket[]}[] = [
    {id: 'novos', title: 'Novos', hint: 'Chamados recém-abertos', icon: 'file', statuses: ['ABERTO'], items: []},
    {id: 'atendimento', title: 'Em atendimento', hint: 'Chamados em andamento', icon: 'settings', statuses: ['EM_ANDAMENTO'], items: []},
    {id: 'aguardando', title: 'Aguardando', hint: 'Aguardando retorno', icon: 'clock', statuses: ['AGUARDANDO_USUARIO', 'AGUARDANDO_TERCEIROS'], items: []},
    {id: 'resolvidos', title: 'Resolvidos', hint: 'Chamados finalizados', icon: 'checkCircle', statuses: ['RESOLVIDO', 'FECHADO'], items: []}
  ];
  private poll?: ReturnType<typeof setInterval>;
  private socket?: WebSocket;
  private stopped = false;
  private socketTries = 0;

  constructor(private api: ApiService, readonly themes: ThemeService, private sanitizer: DomSanitizer) {
    try { this.seen = JSON.parse(sessionStorage.getItem('cr-read') || '{}'); } catch { this.seen = {}; }
  }

  @HostListener('document:click') closeProfile(): void { this.profileOpen = false; this.headerMenu = false; }
  @HostListener('document:keydown.escape') closeMenus(): void { if (this.enlarged) { this.enlarged = null; return; } this.profileOpen = false; this.headerMenu = false; }

  async ngOnInit(): Promise<void> {
    try { this.meta = await this.api.request<Meta>('/meta'); } catch { this.error = 'Não foi possível carregar os departamentos.'; }
    await this.loadBoard(true);
    try { const status = await this.api.request<{message:string}>('/whatsapp/status'); this.whatsappNotice = status.message; } catch { /* o aviso padrão permanece */ }
    this.connectDesk();
    this.poll = setInterval(() => { if (!this.busy) void this.refreshQuiet(); }, 20000);
  }

  ngOnDestroy(): void { this.stopped = true; this.socket?.close(); if (this.poll) clearInterval(this.poll); }

  private connectDesk(): void {
    if (this.stopped || this.socketTries > 5) return;
    const socket = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws/desk');
    this.socket = socket;
    socket.onopen = () => { this.socketTries = 0; };
    socket.onmessage = () => { if (!this.busy) void this.refreshQuiet(); };
    socket.onclose = () => { if (this.stopped || this.socket !== socket) return; this.socketTries++; setTimeout(() => this.connectDesk(), 4000); };
  }

  get admin(): boolean { return this.user.role === 'ADMIN'; }
  get initials(): string { return this.user.name.split(' ').filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase(); }
  get roleLabel(): string { return this.user.role === 'ADMIN' ? 'Administrador de TI' : 'Atendente de TI'; }
  get theme(): 'dark' | 'light' { return this.themes.theme; }
  get headingHint(): string {
    if (this.page === 'painel') return 'Acompanhe e gerencie os chamados da equipe de TI';
    if (this.page === 'perfil') return 'Informações da sua conta na equipe de TI';
    if (this.page === 'conversas') return 'Consulte a solicitação, assuma o atendimento e registre a resolução.';
    return this.whatsappNotice;
  }
  get glance(): string { return this.columns.map(column => column.items.length + ' ' + column.title.toLocaleLowerCase()).join(' · '); }
  get threads(): Ticket[] {
    const query = this.conversationQuery.trim().toLocaleLowerCase();
    return this.tickets
      .filter(ticket => ticket.status !== 'CANCELADO')
      .filter(ticket => !this.activeThreads || !['RESOLVIDO', 'FECHADO'].includes(ticket.status))
      .filter(ticket => !query || (ticket.requester + ' ' + ticket.protocol + ' ' + ticket.department + ' ' + ticket.title).toLocaleLowerCase().includes(query))
      .sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt));
  }

  private viewCache = new Map<string, Ticket[]>();
  toggleTheme(): void { this.themes.toggle(); }
  indicator(id: string): number {
    if (id === 'resolvidosHoje') return this.counts?.['resolvidosHoje'] ?? 0;
    return this.counts?.[id] ?? this.columns.find(column => column.id === id)?.items.length ?? 0;
  }
  countOf(id: string): number {
    const column = this.columns.find(item => item.id === id);
    if (!column) return 0;
    if (this.boardQuery.trim()) return this.shown(column).length;
    return this.counts?.[id] ?? column.items.length;
  }
  shown(column: {id: string; items: Ticket[]}): Ticket[] {
    const query = this.boardQuery.trim().toLocaleLowerCase();
    if (!query) return column.items;
    const key = column.id + '|' + query + '|' + column.items.map(ticket => ticket.id).join(',');
    const cached = this.viewCache.get(key);
    if (cached) return cached;
    const items = column.items.filter(ticket => (ticket.requester + ' ' + ticket.title + ' ' + ticket.protocol + ' ' + ticket.department).toLocaleLowerCase().includes(query));
    this.viewCache.set(key, items);
    return items;
  }
  openProfile(): void { this.headerMenu = false; void this.show('perfil'); }

  async changePassword(): Promise<void> {
    if (this.account.newPassword !== this.account.confirm) { this.notice = 'A confirmação de senha não confere.'; return; }
    this.busy = true;
    this.error = '';
    try {
      await this.api.request('/auth/password', 'POST', this.account);
      this.account = {currentPassword: '', newPassword: '', confirm: ''};
      this.leave.emit();
    } catch (e) { this.fail(e, 'Não foi possível alterar a senha.'); }
    finally { this.busy = false; }
  }

  toggleNav(): void {
    if (window.innerWidth <= 860) this.navOpen = !this.navOpen;
    else { this.collapsed = !this.collapsed; localStorage.setItem('cr-nav', this.collapsed ? '1' : '0'); }
  }

  async show(page: DeskPage): Promise<void> {
    this.page = page;
    this.error = '';
    this.notice = '';
    this.profileOpen = false;
    this.headerMenu = false;
    this.navOpen = false;
    if (page === 'painel' || page === 'conversas' || page === 'equipe') await this.loadBoard(this.tickets.length === 0);
    if (page === 'historico') await this.loadHistory(0);
    if ((page === 'equipe' || page === 'configuracoes') && this.admin) await this.loadAdmin();
  }

  async loadBoard(first: boolean): Promise<void> {
    if (first) this.loading = true;
    try {
      const result = await this.api.request<{items: Ticket[]; counts?: Record<string, number>}>('/tickets?size=500&board=true');
      this.tickets = result.items;
      this.counts = result.counts ?? null;
      this.place();
    } catch (e) {
      this.fail(e, 'Não foi possível carregar os chamados.');
    } finally { this.loading = false; }
  }

  private place(): void {
    for (const column of this.columns) column.items = this.tickets.filter(ticket => column.statuses.includes(ticket.status));
  }

  private async refreshQuiet(): Promise<void> {
    if (document.querySelector('.cdk-drag-dragging')) return;
    if (this.page === 'painel' || this.page === 'conversas' || this.page === 'equipe') await this.loadBoard(false);
    if (this.detail && !this.solution.trim() && !this.reason.trim()) await this.openId(this.detail.ticket.id, true);
  }

  async drop(event: CdkDragDrop<Ticket[]>): Promise<void> {
    if (event.previousContainer === event.container) return;
    const ticket = event.item.data as Ticket;
    await this.move(ticket, event.container.id as ColumnId);
  }

  async move(ticket: Ticket, target: ColumnId): Promise<void> {
    const current = this.columnOf(ticket.status);
    if (current === target) return;
    this.notice = '';
    if (target === 'novos') { this.notice = 'Um chamado não volta para Novos.'; return; }
    if (ticket.status === 'CANCELADO') { this.notice = 'Um chamado cancelado não muda de coluna por aqui.'; return; }
    if (target === 'aguardando') {
      if (ticket.status !== 'EM_ANDAMENTO') { this.notice = 'Passe o chamado para Em atendimento antes de colocá-lo em Aguardando.'; return; }
      await this.patch(ticket, {status: 'AGUARDANDO_USUARIO'});
      return;
    }
    if (target === 'resolvidos') {
      if (!['EM_ANDAMENTO', 'AGUARDANDO_USUARIO', 'AGUARDANDO_TERCEIROS'].includes(ticket.status)) {
        this.notice = 'Finalize um chamado que já está em atendimento ou aguardando.';
        return;
      }
      await this.openId(ticket.id, false);
      this.finishAsk = true;
      this.reopenAsk = false;
      this.notice = 'Descreva a solução para mover o cartão a Resolvidos.';
      return;
    }
    if (ticket.status === 'RESOLVIDO' || ticket.status === 'FECHADO') {
      await this.openId(ticket.id, false);
      this.reopenAsk = true;
      this.finishAsk = false;
      this.notice = 'Informe a justificativa para reabrir o atendimento.';
      return;
    }
    await this.patch(ticket, {status: 'EM_ANDAMENTO'});
  }

  async open(ticket: Ticket): Promise<void> {
    if (this.page !== 'conversas') this.page = 'conversas';
    await this.openId(ticket.id, false);
  }

  async openId(id: number, quiet: boolean): Promise<void> {
    try {
      const detail = await this.api.request<Detail>('/tickets/' + id);
      const same = this.detail?.ticket.id === id;
      this.detail = detail;
      this.seen[String(detail.ticket.id)] = detail.ticket.updatedAt;
      sessionStorage.setItem('cr-read', JSON.stringify(this.seen));
      if (!same) { this.solution = ''; this.reason = ''; this.finishAsk = false; this.reopenAsk = false; this.transferTo = ''; this.enlarged = null; this.brokenFiles = {}; }
    } catch (e) { this.fail(e, 'Não foi possível abrir o chamado.'); }
  }

  closeTalk(): void { this.detail = null; this.finishAsk = false; this.reopenAsk = false; }

  async assume(): Promise<void> {
    if (!this.detail) return;
    const ticket = this.detail.ticket;
    if (ticket.assigneeId && ticket.assigneeId !== this.user.id) { this.notice = 'Este chamado já tem responsável. Use transferir.'; return; }
    await this.patch(ticket, ticket.status === 'ABERTO' ? {status: 'EM_ANDAMENTO'} : {assigneeId: this.user.id});
  }

  async transfer(): Promise<void> {
    if (!this.detail || !this.transferTo) return;
    await this.patch(this.detail.ticket, {assigneeId: Number(this.transferTo)});
    this.transferTo = '';
  }

  async finish(): Promise<void> {
    if (!this.detail) return;
    if (this.solution.trim().length < 10) { this.notice = 'Descreva a solução em pelo menos 10 caracteres.'; return; }
    await this.patch(this.detail.ticket, {status: 'RESOLVIDO', solution: this.solution.trim()});
    this.solution = '';
    this.finishAsk = false;
  }

  async reopen(): Promise<void> {
    if (!this.detail) return;
    if (this.reason.trim().length < 10) { this.notice = 'Informe uma justificativa com pelo menos 10 caracteres.'; return; }
    await this.patch(this.detail.ticket, {status: 'EM_ANDAMENTO', reason: this.reason.trim()});
    this.reason = '';
    this.reopenAsk = false;
  }

  download(id: number, name: string): void { void this.api.download('/attachments/' + id, name).catch(e => this.fail(e, 'Não foi possível baixar o arquivo.')); }
  fileUrl(id: number): string { return '/api/attachments/' + id; }
  safeFile(id: number): SafeResourceUrl { return this.sanitizer.bypassSecurityTrustResourceUrl(this.fileUrl(id)); }
  isImage(mime: string): boolean { return mime.startsWith('image/'); }
  isPdf(mime: string): boolean { return mime === 'application/pdf'; }
  fileSize(bytes: number): string { if (bytes < 1024) return bytes + ' B'; if (bytes < 1048576) return Math.max(1, Math.round(bytes / 1024)) + ' KB'; return (bytes / 1048576).toFixed(1) + ' MB'; }
  markBroken(id: number): void { this.brokenFiles = {...this.brokenFiles, [id]: true}; }
  stageLabel(ticket: Ticket): string {
    if (ticket.status === 'ABERTO') return 'Novo';
    if (ticket.status === 'EM_ANDAMENTO') return 'Em atendimento';
    if (ticket.status === 'AGUARDANDO_USUARIO' || ticket.status === 'AGUARDANDO_TERCEIROS') return 'Aguardando';
    if (ticket.status === 'RESOLVIDO' || ticket.status === 'FECHADO') return 'Resolvido';
    return this.statusLabels[ticket.status];
  }
  priorityTone(ticket: Ticket): string { return ticket.status === 'RESOLVIDO' || ticket.status === 'FECHADO' ? 'resolvido' : ticket.priority; }
  priorityText(ticket: Ticket): string { return ticket.status === 'RESOLVIDO' || ticket.status === 'FECHADO' ? 'Resolvido' : this.priorityLabels[ticket.priority]; }
  canAssume(ticket: Ticket): boolean {
    if (ticket.assigneeId && ticket.assigneeId !== this.user.id) return false;
    if (ticket.assigneeId === this.user.id && ticket.status === 'EM_ANDAMENTO') return false;
    return !!this.detail?.transitions.includes('EM_ANDAMENTO');
  }
  assumeLabel(ticket: Ticket): string {
    if (ticket.assigneeId === this.user.id && ticket.status === 'EM_ANDAMENTO') return 'Você assumiu este chamado';
    if (ticket.assigneeId && ticket.assigneeId !== this.user.id) return 'Com ' + ticket.assignee;
    return 'Assumir o chamado';
  }
  canResolve(ticket: Ticket): boolean { return !!this.detail?.transitions.includes('RESOLVIDO') && ticket.status !== 'RESOLVIDO' && ticket.status !== 'FECHADO'; }
  askFinish(): void { this.finishAsk = true; this.reopenAsk = false; }

  async loadHistory(page: number): Promise<void> {
    this.historyPage = page;
    const params = new URLSearchParams({page: String(page), size: '12'});
    if (this.historyQuery.trim()) params.set('q', this.historyQuery.trim());
    if (this.historyStatus) params.set('status', this.historyStatus);
    if (this.historyDepartment) params.set('department', this.historyDepartment);
    if (this.historyAssignee) params.set('assignee', this.historyAssignee);
    if (this.historyPriority) params.set('priority', this.historyPriority);
    try {
      const result = await this.api.request<{items: Ticket[]; total: number; pages: number}>('/tickets?' + params.toString());
      this.history = result.items;
      this.historyTotal = result.total;
      this.historyPages = result.pages;
    } catch (e) { this.fail(e, 'Não foi possível consultar o histórico.'); }
  }

  async loadAdmin(): Promise<void> {
    try {
      const [users, catalogs, summary] = await Promise.all([
        this.api.request<Person[]>('/admin/users'),
        this.api.request<{departments: Item[]}>('/admin/catalogs'),
        this.api.request<Summary>('/dashboard')
      ]);
      this.users = users;
      this.departments = catalogs.departments;
      this.summary = summary;
    } catch (e) { this.fail(e, 'Não foi possível abrir a administração.'); }
  }

  async createUser(): Promise<void> {
    if (this.newUser.name.trim().length < 2 || !this.newUser.email.includes('@') || !this.newUser.departmentId || this.newUser.password.length < 12) {
      this.notice = 'Informe nome, e-mail, departamento e uma senha com 12 caracteres, maiúsculas, minúsculas e números.';
      return;
    }
    this.busy = true;
    try {
      await this.api.request('/admin/users', 'POST', {...this.newUser, departmentId: Number(this.newUser.departmentId), active: true});
      this.newUser = {name: '', email: '', role: 'TECNICO', departmentId: '', password: ''};
      await this.loadAdmin();
      this.notice = 'Atendente cadastrado. A senha temporária deverá ser trocada no primeiro acesso.';
    } catch (e) { this.fail(e, 'Não foi possível cadastrar o atendente.'); }
    finally { this.busy = false; }
  }

  async toggleUser(person: Person): Promise<void> {
    this.busy = true;
    try {
      await this.api.request('/admin/users/' + person.id, 'PUT', {name: person.name, email: person.email, role: person.role, departmentId: person.departmentId, password: '', active: !person.active});
      await this.loadAdmin();
    } catch (e) { this.fail(e, 'Não foi possível atualizar a conta.'); }
    finally { this.busy = false; }
  }

  async createDepartment(): Promise<void> {
    const name = this.newDepartment.trim();
    if (name.length < 2) return;
    this.busy = true;
    try {
      await this.api.request('/admin/departments', 'POST', {name, description: '', active: true});
      this.newDepartment = '';
      await this.loadAdmin();
    } catch (e) { this.fail(e, 'Não foi possível cadastrar o departamento.'); }
    finally { this.busy = false; }
  }

  async toggleDepartment(item: Item): Promise<void> {
    this.busy = true;
    try {
      await this.api.request('/admin/departments/' + item.id, 'PUT', {name: item.name, description: item.description || '', active: !item.active});
      await this.loadAdmin();
    } catch (e) { this.fail(e, 'Não foi possível atualizar o departamento.'); }
    finally { this.busy = false; }
  }

  unread(ticket: Ticket): boolean { return !!ticket.awaitingReply && this.seen[String(ticket.id)] !== ticket.updatedAt; }
  channelLabel(ticket: Ticket): string { return ticket.channel === 'WHATSAPP' ? 'WhatsApp' : 'Painel'; }
  assignedCount(id: number): number { return this.tickets.filter(ticket => ticket.assigneeId === id && !['RESOLVIDO', 'FECHADO', 'CANCELADO'].includes(ticket.status)).length; }

  preview(ticket: Ticket): string {
    const text = ticket.description.replace(/\s+/g, ' ').trim();
    return text.length > 96 ? text.slice(0, 96) + '…' : text;
  }

  mine(ticket: Ticket): boolean { return ticket.assigneeId === this.user.id; }

  locked(ticket: Ticket): boolean { return ticket.status === 'FECHADO' || ticket.status === 'CANCELADO'; }

  private columnOf(status: Status): ColumnId | '' {
    return this.columns.find(column => column.statuses.includes(status))?.id || '';
  }

  private async patch(ticket: Ticket, body: Record<string, unknown>): Promise<void> {
    this.busy = true;
    this.error = '';
    try {
      await this.api.request('/tickets/' + ticket.id, 'PATCH', {version: ticket.version, ...body});
      await this.loadBoard(false);
      if (this.detail?.ticket.id === ticket.id) await this.openId(ticket.id, true);
      if (this.page === 'historico') await this.loadHistory(this.historyPage);
    } catch (e) {
      this.fail(e, 'Não foi possível atualizar o chamado.');
      await this.loadBoard(false);
    } finally { this.busy = false; }
  }

  private fail(error: unknown, fallback: string): void {
    this.error = error instanceof ApiError ? error.message : fallback;
  }
}
