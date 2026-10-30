package br.com.chamadorapido;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
@Component
@ConfigurationProperties(prefix="app.whatsapp")
public class WhatsAppProperties {
  private String token="";
  private String phoneNumberId="";
  private String verifyToken="";
  private String appSecret="";
  private String graphVersion="v21.0";
  public String getToken(){return token;}
  public void setToken(String token){this.token=token==null?"":token.trim();}
  public String getPhoneNumberId(){return phoneNumberId;}
  public void setPhoneNumberId(String phoneNumberId){this.phoneNumberId=phoneNumberId==null?"":phoneNumberId.trim();}
  public String getVerifyToken(){return verifyToken;}
  public void setVerifyToken(String verifyToken){this.verifyToken=verifyToken==null?"":verifyToken.trim();}
  public String getAppSecret(){return appSecret;}
  public void setAppSecret(String appSecret){this.appSecret=appSecret==null?"":appSecret.trim();}
  public String getGraphVersion(){return graphVersion;}
  public void setGraphVersion(String graphVersion){this.graphVersion=graphVersion==null?"":graphVersion.trim();}
  public boolean receivingReady(){return !verifyToken.isBlank()&&!appSecret.isBlank();}
  public boolean sendingReady(){return !token.isBlank()&&phoneNumberId.matches("\\d{6,24}");}
  public boolean configured(){return receivingReady()&&sendingReady();}
  public String graphVersionOrDefault(){return graphVersion.matches("v\\d+\\.\\d+")?graphVersion:"v21.0";}
}
