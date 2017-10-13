package us.dot.its.jpo.ode.coder.stream;

import java.io.BufferedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dot.its.jpo.ode.coder.OdeDataPublisher;
import us.dot.its.jpo.ode.importer.ImporterDirectoryWatcher.ImporterFileType;
import us.dot.its.jpo.ode.importer.parser.BsmLogFileParser;
import us.dot.its.jpo.ode.importer.parser.DistressMsgFileParser;
import us.dot.its.jpo.ode.importer.parser.FileParser.ParserStatus;
import us.dot.its.jpo.ode.importer.parser.LogFileParser;
import us.dot.its.jpo.ode.importer.parser.RxMsgFileParser;
import us.dot.its.jpo.ode.importer.parser.TimLogFileParser;
import us.dot.its.jpo.ode.model.OdeData;

public class BinaryDecoderPublisher extends AbstractDecoderPublisher {

   private static final String BSMS_FOR_EVENT_PREFIX = "bsmLogDuringEvent";
   private static final String RECEIVED_MESSAGES_PREFIX = "rxMsg";
   private static final String DISTRESS_MESSAGES_PREFIX = "dnMsg";

   private static final Logger logger = LoggerFactory.getLogger(BinaryDecoderPublisher.class);
   private OdeDataPublisher timMessagePublisher;

   public BinaryDecoderPublisher(
      OdeDataPublisher bsmMessagePublisher, OdeDataPublisher timMessagePublisher) {
      super(bsmMessagePublisher);
      this.timMessagePublisher = timMessagePublisher;
   }

   @Override
   public void decodeAndPublish(BufferedInputStream bis, String fileName, ImporterFileType fileType) throws Exception {
      OdeData decoded = null;

      LogFileParser fileParser = null;

      do {
         try {

            // Step 1 - parse and decode
            ParserStatus status = ParserStatus.UNKNOWN;

            if (fileType == ImporterFileType.BSM_LOG_FILE) {
               if (fileName.startsWith(BSMS_FOR_EVENT_PREFIX)) {
                  logger.debug("Parsing as \"BSM For Event\" log file type.");
                  fileParser = new BsmLogFileParser(bundleId.incrementAndGet());
               } else if (fileName.startsWith(RECEIVED_MESSAGES_PREFIX)) {
                  logger.debug("Parsing as \"Received Messages\" log file type.");
                  fileParser = new RxMsgFileParser(bundleId.incrementAndGet());
               } else if (fileName.startsWith(DISTRESS_MESSAGES_PREFIX)) {
                  logger.debug("Parsing as \"Distress Notifications\" log file type.");
                  fileParser = new DistressMsgFileParser(bundleId.incrementAndGet());
               } else {
                  throw new IllegalArgumentException("Unknown log file prefix: " + fileName);
               }

               status = fileParser.parseFile(bis, fileName);
               if (status == ParserStatus.COMPLETE) {
                  try {
                     if (fileParser instanceof BsmLogFileParser) {
                        logger.debug("Attempting to decode log file message as a BSM...");
                        decoded = bsmDecoder.decode((BsmLogFileParser) fileParser,
                              this.serialId.setBundleId(bundleId.get()));
                     } else if (fileParser instanceof RxMsgFileParser) {
                        logger.debug("Attempting to decode log file message as a rxTIM...");
                        decoded = timDecoder.decode((RxMsgFileParser) fileParser,
                              this.serialId.setBundleId(bundleId.get()));
                     } else if (fileParser instanceof DistressMsgFileParser) {
                        logger.debug("Attempting to decode log file message as a distress TIM...");
                        decoded = timDecoder.decode((DistressMsgFileParser) fileParser,
                              this.serialId.setBundleId(bundleId.get()));
                     }
                  } catch (Exception e) {
                     logger.debug("Failed to decode log file message.", e);

                  }
               } else {
                  logger.debug("Status = {}", status);
                  return;
               }

            } else {
                decoded = bsmDecoder.decode(bis, fileName, 
                    this.serialId.setBundleId(bundleId.get()).addRecordId(1));
                status = ParserStatus.NA;
            }

            // Step 2 - publish
            if (decoded != null) {
               if (fileType == ImporterFileType.BSM_LOG_FILE) {
                  if (fileParser instanceof BsmLogFileParser) {
                     logger.debug("Decoded a bsm: {}", decoded);
                     bsmMessagePublisher.publish(decoded, 
                        bsmMessagePublisher.getOdeProperties().getKafkaTopicOdeBsmPojo());
                  } else if (fileType == ImporterFileType.BSM_LOG_FILE && fileParser instanceof TimLogFileParser) {
                     logger.debug("Decoded a tim: {}", decoded);
                     timMessagePublisher.publish(decoded, 
                        bsmMessagePublisher.getOdeProperties().getKafkaTopicOdeTimPojo());
                  }
               }
            } else {
               // if parser returns PARTIAL record, we will go back and continue
               // parsing
               // but if it's UNKNOWN, it means that we could not parse the
               // header bytes
               if (status == ParserStatus.INIT) {
                  logger.error("Failed to parse the header bytes.");
               } else {
                  logger.error("Failed to decode ASN.1 data");
               }
            }
         } catch (Exception e) {
            logger.error("Error decoding and publishing data.", e);
         }
      } while (decoded != null);
   }

}