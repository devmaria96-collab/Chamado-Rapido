package br.com.chamadorapido;
public interface WhatsAppClient {
  Result sendText(String to,String body);
  record Result(boolean accepted,String externalId,String error) {
    static Result ok(String externalId){return new Result(true,externalId,null);}
    static Result fail(String error){return new Result(false,null,error);}
  }
}
