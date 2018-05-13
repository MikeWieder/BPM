package autokauf;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.camunda.bpm.BpmPlatform;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.FileValue;
import org.camunda.bpm.engine.RuntimeService;


public class BenachrichtigungKaufvertrag implements TaskListener {

  // TODO: Set Mail Server Properties
  private static final String HOST = "mail.opentrash.com";
  private static final String USER = "camundaexample@opentrash.com";
  private static final String PWD = "password";

  private final static Logger LOGGER = Logger.getLogger(BenachrichtigungKaufvertrag.class.getName());

  public void notify(DelegateTask delegateTask) {

    String assignee = delegateTask.getAssignee();
    String taskId = delegateTask.getId();

    if (assignee != null) {
    
      // Get User Profile from User Management
      IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
      User user = identityService.createUserQuery().userId(assignee).singleResult();

      if (user != null) {
      
        // Get Email Address from User Profile
        //String recipient = user.getEmail();
    	  String recipient = (String)delegateTask.getVariable("kundenmail");
      
        if (recipient != null && !recipient.isEmpty()) {
        	
        	StringBuilder sb = new StringBuilder();
        	ProcessEngine processEngine = BpmPlatform.getDefaultProcessEngine();
        	RuntimeService runtimeService = processEngine.getRuntimeService();
        	
			String knummer = (String) delegateTask.getVariable("kundennummer");
        	String filename = "Kaufvertrag"+knummer;
        	sb.append((String)delegateTask.getVariable("kundennummer"));
        	
        	sb.append((String)delegateTask.getVariable("kundenmail"));
        	sb.append((delegateTask.getVariable("preis")).toString());
        	String message = sb.toString();
        	
        	try (PDDocument doc = new PDDocument())
        		        {
        		            PDPage page = new PDPage();
        		            doc.addPage(page);
        		            
        		            PDFont font = PDType1Font.HELVETICA_BOLD;
        		
        		            try (PDPageContentStream contents = new PDPageContentStream(doc, page))
        		            {
        		                contents.beginText();
        		                contents.setFont(font, 12);
        		                contents.newLineAtOffset(100, 700);
        		                contents.showText(message);
        		                contents.newLine();
        		                contents.drawString(message);
        		                contents.endText();
        		            } catch (IOException e) {
								e.printStackTrace();
							}
        		            
        		            doc.save(filename+".pdf");
        		            FileValue kaufvertragFile = Variables.fileValue(filename+".pdf").
        		            		file(new File(filename+".pdf")).mimeType("text/plain").
        		            		encoding("UTF-8").create();
        		            runtimeService.setVariable(delegateTask.getProcessInstanceId(),"kaufvertrag",kaufvertragFile);
        		            doc.save(filename+".pdf");
        		        } catch (IOException e) {
							e.printStackTrace();
						}

        	EmailAttachment attachment = new EmailAttachment();
            attachment.setPath(filename+".pdf");
            attachment.setDisposition(EmailAttachment.ATTACHMENT);
            attachment.setDescription("PDF des Kaufvertrags");
            attachment.setName(filename+".pdf");
        	
          MultiPartEmail email = new MultiPartEmail();
          email.setHostName(HOST);
          email.setAuthentication(USER, PWD);

          try {
            email.setFrom(USER);
            email.setSubject("Bitte Bestaetigen Sie den Kaufvertrag");
            email.setMsg("Bitte bestaetigen Sie Ihren Kaufvertrag unter : http://localhost:8080/camunda/app/tasklist/default/#/task/" + taskId);

            email.addTo(recipient);
            
            email.attach(attachment);

            email.send();
            LOGGER.info("Task Assignment Email successfully sent to user '" + assignee + "' with address '" + recipient + "'.");           

          } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not send email to assignee", e);
          }

        } else {
          LOGGER.warning("Not sending email to user " + assignee + "', user has no email address.");
        }

      } else {
        LOGGER.warning("Not sending email to user " + assignee + "', user is not enrolled with identity service.");
      }

    }

  }

}