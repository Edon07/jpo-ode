package us.dot.its.jpo.ode.pdm;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.ScopedPDU;
import org.snmp4j.event.ResponseEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.snmp.SnmpSession;
import us.dot.its.jpo.ode.util.JsonUtils;

@Controller
public class PdmController {

   private static Logger logger = LoggerFactory.getLogger(PdmController.class);

   @ResponseBody
   @RequestMapping(value = "/pdm", method = RequestMethod.POST, produces = "application/json")
   public ResponseEntity<String> pdmMessage(@RequestBody String jsonString) {
      if (null == jsonString) {
         logger.error("PDM controller received empty request");
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empty request");
      }

      J2735PdmRequest pdmRequest = (J2735PdmRequest) JsonUtils.fromJson(jsonString, J2735PdmRequest.class);

      String jsonPdmRequest = pdmRequest.toJson(true);
      logger.debug("J2735PdmRequest: {}", jsonPdmRequest);

      String aggregateBodyMessage = "[";
      ScopedPDU pdu = PdmUtil.createPDU(pdmRequest.getPdm());
      for (RSU curRsu : pdmRequest.getRsuList()) {

         String curRsuMessage = "Error";

         ResponseEvent response = null;
         try {
            response = createAndSend(pdu, curRsu);
            if (null == response || null == response.getResponse()) {
               curRsuMessage = "Timeout";
            } else if (0 == response.getResponse().getErrorStatus()) {
               curRsuMessage = "Deposit successful";
            } else {
               curRsuMessage = "Deposit failed: " + response.getResponse().getErrorStatusText();
            }
         } catch (IOException e) {
            logger.error("Exception sending PDM: ", e);
            curRsuMessage = "Exception occurred";
         }

         if (!("[".equals(aggregateBodyMessage))) {
            // Add a comma after the first message
            aggregateBodyMessage = aggregateBodyMessage.concat(",");
         }

         aggregateBodyMessage = aggregateBodyMessage.concat("{\"" + curRsu.getRsuTarget() + "\":\"" + curRsuMessage + "\"}");
      }

      aggregateBodyMessage = aggregateBodyMessage.concat("]");
      aggregateBodyMessage = "{\"rsu_responses\":".concat(aggregateBodyMessage).concat("}");

      return ResponseEntity.status(HttpStatus.OK).body(aggregateBodyMessage);
   }

   public static ResponseEvent createAndSend(ScopedPDU pdu, RSU rsu) throws IOException {
      SnmpSession session = new SnmpSession(rsu);

      return session.set(pdu, session.getSnmp(), session.getTarget(), false);
   }

}
