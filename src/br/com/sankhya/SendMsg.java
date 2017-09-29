package br.com.sankhya;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.microtripit.mandrillapp.lutung.MandrillApi;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.MessageContent;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.Recipient;
import com.microtripit.mandrillapp.lutung.view.MandrillMessage.Recipient.Type;
import com.microtripit.mandrillapp.lutung.view.MandrillMessageStatus;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;

public class SendMsg

{
	private static String getDateTime() 
	{ 
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); 
		Date date = new Date(); 
		return dateFormat.format(date); 
	}
	
	public static Object ExecutaComandoNoBanco(String sql, String op)
	{
		try
		{
			Statement smnt = ConnectMSSQLServer.conn.createStatement(); 
			
			if(op=="select")
			{
				smnt.execute(sql);
				ResultSet result = smnt.executeQuery(sql); 
				result.next();
				return result.getObject(1);
			}
			else if(op=="alter")
			{
				smnt.executeUpdate(sql);
				return (Object)1;
			}
			else
			{
				return null;
			}
		}
		catch(SQLException ex)
		{
			System.err.println("SQLException: " + ex.getMessage());

			return null;
		}
	}	

	public static void main(String[] args) throws IOException 

	{
		String emailParc="", nomeVend="", emailVend="";
		BigDecimal nunota=BigDecimal.ZERO, tipoOp=BigDecimal.ZERO, codParc=BigDecimal.ZERO, 
		    	   codVend=BigDecimal.ZERO, codContato=BigDecimal.ZERO;
		int codSMTPVend=0, ramalVend=0, ultIdEmailMonitor=0;
		MandrillApi mandrillApi = new MandrillApi("5ZbB4mED1LcYaISfhPtquQ");

		//Conecta no banco do Sankhya
		ConnectMSSQLServer.dbConnect("jdbc:sqlserver://192.168.0.5:1433;DatabaseName=SANKHYA_TREINA;", "adriano","Compiles23");

		//recupera o numero da negociação
		/*Registro[] linha = contexto.getLinhas();

		for (int i = 0; i < linha.length; i++) 
		{
			nunota= (BigDecimal)linha[i].getCampo("NUNOTA");
			tipoOp=(BigDecimal) linha[i].getCampo("CODTIPOPER");
			codParc=(BigDecimal) linha[i].getCampo("CODPARC");
			codVend=(BigDecimal) linha[i].getCampo("CODVEND");
			codContato=(BigDecimal) linha[i].getCampo("CODCONTATO");
		}*/

		//Só executa nas TOPs permitidas   
		if (tipoOp.intValue()!=204)
		{	
			String teste;
			//Recupera o ramal do vendedor
			if(ExecutaComandoNoBanco("SELECT AD_TEXTOADCIONALEMAILPROP FROM TGFCAB WHERE nunota="
					+122891, "select")!=null)
			{
				teste=(String) ExecutaComandoNoBanco("SELECT AD_TEXTOADCIONALEMAILPROP FROM TGFCAB WHERE nunota="
						+122891, "select");
				
				System.out.println(teste.toString());
			}
			
			//Recupera o ramal do vendedor
			if(ExecutaComandoNoBanco("SELECT AD_RAMAL FROM TGFVEN WHERE CODVEND="
					+1101, "select")!=null)
			{
				ramalVend=(Integer) ExecutaComandoNoBanco("SELECT AD_RAMAL FROM TGFVEN WHERE CODVEND="
						+1101, "select");
			}
			
			//Recupera ultimo ID da tabela de monitoramento de emails
			if(ExecutaComandoNoBanco("SELECT MAX(CODEMAILMONITOR) FROM AD_EMAILMONITOR", "select")!=null)
			{
				ultIdEmailMonitor=(Integer) ExecutaComandoNoBanco("SELECT MAX(CODEMAILMONITOR) FROM "
						                                + "AD_EMAILMONITOR", "select");
			}
			
			//Recupera o email do vendedor
			if(ExecutaComandoNoBanco("SELECT EMAIL FROM TSIUSU WHERE CODUSU="
					+263, "select")!=null)
			{
				emailVend=(String) ExecutaComandoNoBanco("SELECT EMAIL FROM TSIUSU WHERE CODUSU="
						+263, "select");
			}

			//Recupera o codigo do smtp do vendedor que está enviando
			if(ExecutaComandoNoBanco("SELECT CODSMTP FROM TSISMTP WHERE REMETENTE LIKE"+
					"(SELECT CAST('%' AS VARCHAR)+CAST(REPLACE(EMAIL, ' ', '') AS VARCHAR)+CAST('%' AS VARCHAR)"+
					"FROM TSIUSU WHERE CODUSU="+263+")", "select")!=null)
			{
				codSMTPVend=(Short) (ExecutaComandoNoBanco("SELECT CODSMTP FROM TSISMTP WHERE REMETENTE LIKE"+
						"(SELECT CAST('%' AS VARCHAR)+CAST(REPLACE(EMAIL, ' ', '') AS VARCHAR)+CAST('%' AS VARCHAR)"+
						"FROM TSIUSU WHERE CODUSU="+263+")", "select"));
			}

			//Recupera o nome do vendedor
			if(ExecutaComandoNoBanco("SELECT FUN.NOMEFUNC FROM TGFVEN VEN"+
					" INNER JOIN TSIUSU USU ON USU.CODVEND = VEN.CODVEND"+
					" INNER JOIN TFPFUN FUN ON FUN.CODFUNC = USU.CODFUNC AND FUN.CODEMP=3"+
					" WHERE  USU.CODVEND="+1101, "select")!=null)
			{
				nomeVend=(String) ExecutaComandoNoBanco("SELECT FUN.NOMEFUNC FROM TGFVEN VEN"+
						" INNER JOIN TSIUSU USU ON USU.CODVEND = VEN.CODVEND"+
						" INNER JOIN TFPFUN FUN ON FUN.CODFUNC = USU.CODFUNC AND FUN.CODEMP=3"+
						" WHERE  USU.CODVEND="+1101, "select");
			}

			//Recupera o email do parceiro
			if(ExecutaComandoNoBanco("SELECT CTT.EMAIL FROM TGFCAB CAB"
					+ " INNER JOIN TGFCTT CTT ON CTT.CODCONTATO=CAB.CODCONTATO"
					+ " AND CTT.CODPARC=CAB.CODPARC"
					+ " WHERE CAB.CODCONTATO="+1+" AND CAB.CODPARC="
					+15808, "select")!=null)
			{
				emailParc=(String) ExecutaComandoNoBanco("SELECT CTT.EMAIL FROM TGFCAB CAB"
						+ " INNER JOIN TGFCTT CTT ON CTT.CODCONTATO=CAB.CODCONTATO"
						+ " AND CTT.CODPARC=CAB.CODPARC"
						+ " WHERE CAB.CODCONTATO="+1+" AND CAB.CODPARC="
						+15808, "select");
			}
			
			emailParc = emailParc.replaceAll(" ","");
			emailVend = emailVend.replaceAll(" ","");

			// create your message
			MandrillMessage message = new MandrillMessage();

			message.setSubject("Proposta Comercial - "+nunota.toString());
			message.setHtml("<html><body style="+"\"font-famaly: arial; font-size:12px;"+"\">Prezado(s),<br/><br/>"+		             
					"Segue proposta(s) de fornecimento do(s) material(ais) importado(s) e comercializado(s) pela Medika.<br/><br/>"+
					"Caso deseje visualizar nossos catálogos de produtos clique neste link: <a href="+
					"\"https://1drv.ms/f/s!AhNxsawi2mh1q1G2xpw7_AtidD3m\">Catálogos Medika</a>"+
					"<br/><br/>"+
					"Atenciosamente,"+
					"<br/><br/>"+nomeVend+
					" - Tel:(31) 3688-1901 Ramal:"+ramalVend+" - Equipe de Vendas"+
					"<br><br><HR WIDTH=100% style="+"\"border:1px solid #191970;"+
					"\"><img src="+"\"http://www.medika.com.br/wp-content/uploads/2016/05/logo-medika.png"+
					"\"><br><br>Medika, qualidade em saúde. - <a href="+"\"http://www.medika.com.br"+
					"\">www.medika.com.br</a><br>"+
					"<HR WIDTH=100% style="+"\"border:1px solid #191970;"+"\">"+
					"</body></html>");
			message.setAutoText(true);
			message.setFromEmail(emailVend);
			message.setFromName("Equipe de Vendas - Medika");
			// add recipients
			ArrayList<Recipient> recipients = new ArrayList<Recipient>();
			Recipient recipient = new Recipient();
			recipient.setEmail(emailParc/*"ovokindersurpresa@gmail.com"*/);
			recipient.setName("-");
			recipients.add(recipient);
			
			//EMAIL CÓPIA
			Recipient recipient2 = new Recipient();
			recipient2.setEmail("gadrianosl@gmail.com");
			recipient2.setName("-");
			recipient2.setType(Type.CC);
			recipients.add(recipient2);
			
			//EMAIL CÓPIA OCULTA
			Recipient recipient3 = new Recipient();
			recipient3.setEmail("ovokindersurpresa@gmail.com");
			recipient3.setName("-");
			recipient3.setType(Type.CC);
			recipients.add(recipient3);
			
			
			message.setTo(recipients);
			message.setPreserveRecipients(true);
			message.setTrackOpens(true);
			message.setTrackClicks(true);

			//Tratamento para adicionar anexo na mensagem
			List<MessageContent> listofAttachments = new ArrayList<MessageContent>();
			MessageContent attachment = new MessageContent();
			attachment.setType("application/pdf");
			attachment.setName("Proposta Comercial.pdf");

			//Criação do anexo da proposta
			//GeradorDeRelatorios.geraPdf("/home/mgeweb/modelos/relatorios/propostadevenda/PEDIDO_DE_VENDA1x.jrxml", nunota);  
			GeradorDeRelatorios.geraPdf("/users/adriano/relatorios/propvenda/PEDIDO_DE_VENDA1x.jrxml", nunota.add(new BigDecimal(122814)));

			File file = new File("/users/adriano/relatorios/propvenda/propvenda.pdf");
			//File file = new File("/home/mgeweb/modelos/relatorios/propostadevenda/propvenda.pdf");

			InputStream is = new FileInputStream(file);

			long length = file.length();
			if (length > Integer.MAX_VALUE) 
			{
				// File is too large
			}
			byte[] bytes = new byte[(int) length];

			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) 
			{
				offset += numRead;
			}

			if (offset < bytes.length) 
			{
				throw new IOException("Não foi possível completar a leitura do arquivo. " + file.getName());
			}

			is.close();
			byte[] encoded = Base64.encodeBase64(bytes);
			String encodedString = new String(encoded);
			attachment.setContent(encodedString);
			listofAttachments.add(attachment);      

			message.setAttachments(listofAttachments);
			ArrayList<String> tags = new ArrayList<String>();
			tags.add("122884");
			tags.add(Integer.toString(ultIdEmailMonitor+1));
			message.setTags(tags);
			// ... add more message details if you want to!
			// then ... send
			try {
				MandrillMessageStatus[] messageStatusReports = mandrillApi
						.messages().send(message, false);

				StringBuffer mensagem = new StringBuffer();
				mensagem.append("Email enviado com Mandrill! \n\n");
                Date dataAtual=new Date();
				//Insere log de envio no banco
				try
				{
				  if(ExecutaComandoNoBanco("INSERT INTO AD_EMAILMONITOR (NUNOTA, STATUSENVIO, DHENVIO, DESTINATARIO, REMETENTE)VALUES("+122884+",'ENVIADO', '"+getDateTime()+"','"+ emailParc+"','"+emailVend+"')", "alter")!=null)
				  {
					mensagem.append("Adicionado um registro na tabela de log de envios.");
				  }

				}
				catch (Exception e) 
				{
					mensagem.append("Erro ao inserir na registro na tabela de log de envios. "+e.getMessage());
				}

				System.out.println(mensagem);

			} catch (MandrillApiError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			StringBuffer mensagem = new StringBuffer();
			mensagem.append("O envio da Proposta de Venda e Apresentação Comercial deve ser feito apenas na TOP 204!");

			System.out.println(mensagem.toString());  
		}

	}

}
