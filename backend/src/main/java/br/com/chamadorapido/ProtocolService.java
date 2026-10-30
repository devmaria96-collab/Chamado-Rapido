package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Numeração do protocolo do ciclo atual. O dia seguinte não reinicia a sequência. */
@Service
public class ProtocolService {
  static final List<Status> ACTIVE=List.of(Status.ABERTO,Status.EM_ANDAMENTO,Status.AGUARDANDO_USUARIO,Status.AGUARDANDO_TERCEIROS);
  private final TicketRepository tickets;
  private final AttendanceCycleRepository cycles;
  public ProtocolService(TicketRepository tickets,AttendanceCycleRepository cycles){this.tickets=tickets;this.cycles=cycles;}
  @Transactional
  public void assign(Ticket ticket){
    AttendanceCycle cycle=cycles.lock().orElseThrow(()->UserService.error(500,"A sequência de protocolos não está disponível."));
    boolean restart=tickets.count()>0&&tickets.countByStatusIn(ACTIVE)==0;
    if(restart){cycle.cycleNo++;cycle.nextNo=1;}
    ticket.protocolCycle=cycle.cycleNo;
    ticket.protocolNo=cycle.nextNo;
    cycle.nextNo++;
    cycles.save(cycle);
  }
}
