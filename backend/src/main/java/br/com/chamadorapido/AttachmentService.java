package br.com.chamadorapido;
import br.com.chamadorapido.Domain.*;
import static br.com.chamadorapido.UserService.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
@Service
@Transactional
public class AttachmentService {
  private final UserService auth; private final WorkService work; private final AttachmentRepository attachments; private final Path storage;
  public AttachmentService(UserService auth,WorkService work,AttachmentRepository attachments,@Value("${app.storage}")String storage){this.auth=auth;this.work=work;this.attachments=attachments;this.storage=Path.of(storage).toAbsolutePath().normalize();}
  public void upload(Long id,MultipartFile file)throws IOException{
    User u=auth.operational();Ticket t=work.locked(id,u);if(TicketRules.terminal(t.status.name()))throw error(400,"Reabra o chamado antes de anexar arquivos.");if(attachments.countByTicketId(id)>=5)throw error(400,"Limite de 5 anexos por chamado.");if(file.isEmpty()||file.getSize()>5L*1024*1024)throw error(400,"Anexe um arquivo entre 1 byte e 5 MB.");
    byte[] data=file.getBytes();String mime=detect(data);String name=Optional.ofNullable(file.getOriginalFilename()).orElse("arquivo").replaceAll("[\\p{Cntrl}\\\\/]","_");if(name.length()>180)name=name.substring(name.length()-180);String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toLowerCase(Locale.ROOT):"";
    if(!(mime.equals("image/png")&&ext.equals("png")||mime.equals("image/jpeg")&&Set.of("jpg","jpeg").contains(ext)||mime.equals("application/pdf")&&ext.equals("pdf")||mime.equals("text/plain")&&ext.equals("txt")))throw error(400,"Formato inválido. Use PNG, JPG, PDF ou TXT com extensão correspondente.");
    Files.createDirectories(storage);String key=UUID.randomUUID().toString();Path target=storage.resolve(key);Files.write(target,data,StandardOpenOption.CREATE_NEW);
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(target);}catch(IOException ignored){}}});
    Attachment a=new Attachment();a.ticket=t;a.author=u;a.originalName=name;a.storageKey=key;a.mime=mime;a.size=data.length;attachments.saveAndFlush(a);work.history(t,u,"Arquivo anexado",name);
  }
  private String detect(byte[] data){if(data.length>=8&&Arrays.equals(Arrays.copyOf(data,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";if(data.length>=3&&(data[0]&255)==255&&(data[1]&255)==216&&(data[2]&255)==255)return "image/jpeg";if(data.length>=5&&new String(data,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))return "application/pdf";try{String txt=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString();if(txt.indexOf('\0')<0)return "text/plain";}catch(CharacterCodingException ignored){}throw error(400,"Conteúdo de arquivo não permitido.");}
  public record Download(Attachment metadata,Path path){}
  @Transactional(readOnly=true) public Download download(Long id){User u=auth.operational();Attachment a=attachments.findById(id).orElseThrow(()->error(404,"Anexo não encontrado."));work.visible(a.ticket.id,u);Path path=storage.resolve(a.storageKey).normalize();if(!path.startsWith(storage)||!Files.isRegularFile(path))throw error(404,"Arquivo indisponível no armazenamento.");return new Download(a,path);}
}
