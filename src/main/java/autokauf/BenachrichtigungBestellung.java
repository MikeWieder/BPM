package autokauf;

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
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.impl.context.Context;

public class BenachrichtigungBestellung implements TaskListener {

  private static final String HOST = "mail.opentrash.com";
  private static final String USER = "camundaexample@opentrash.com";
  private static final String PWD = "password";

  private final static Logger LOGGER = Logger.getLogger(BenachrichtigungKaufvertrag.class.getName());

  public void notify(DelegateTask delegateTask) {

    String assignee = delegateTask.getAssignee();
    String taskId = delegateTask.getId();
    String processId = delegateTask.getProcessInstanceId();

    if (assignee != null) {
    
      // Get User Profile from User Management
      IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
      User user = identityService.createUserQuery().userId(assignee).singleResult();

      if (user != null) {
      
        //Get Email Address from User Profile
        //String recipient = user.getEmail();
  	    String recipient = (String)delegateTask.getVariable("kundenmail");
      
        if (recipient != null && !recipient.isEmpty()) {
        	
        	StringBuilder sb = new StringBuilder();
        	
        	String filename = "Bestellung"+processId;
        	sb.append((String)delegateTask.getVariable("kundennummer"));
        	sb.append("----------------------------------------------------");
        	sb.append((String)delegateTask.getVariable("kundenmail"));
        	sb.append("----------------------------------------------------");
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
        		            
        		            doc.save(filename);
        		        } catch (IOException e) {
							e.printStackTrace();
						}
        	

          EmailAttachment attachment = new EmailAttachment();
          attachment.setPath(filename);
          attachment.setDisposition(EmailAttachment.ATTACHMENT);
          attachment.setDescription("PDF der Bestellung");
          attachment.setName(filename+".pdf");
        	
          MultiPartEmail email = new MultiPartEmail();
          email.setHostName(HOST);
          email.setAuthentication(USER, PWD);

          try {
            email.setFrom(USER);
            email.setSubject("Task assigned: " + delegateTask.getName());
            email.setMsg("Please complete: http://localhost:8080/camunda/app/tasklist/default/#/task/" + taskId);

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