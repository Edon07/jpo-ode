/*******************************************************************************
 * Copyright (c) 2015 US DOT - Joint Program Office
 *
 * The Government has unlimited rights to all documents/material produced under 
 * this task order. All documents and materials, to include the source code of 
 * any software produced under this contract, shall be Government owned and the 
 * property of the Government with all rights and privileges of ownership/copyright 
 * belonging exclusively to the Government. These documents and materials may 
 * not be used or sold by the Contractor without written permission from the CO.
 * All materials supplied to the Government shall be the sole property of the 
 * Government and may not be used for any other purpose. This right does not 
 * abrogate any other Government rights.
 *
 * Contributors:
 *     Booz | Allen | Hamilton - initial API and implementation
 *******************************************************************************/
package us.dot.its.jpo.ode.model;

public class OdeData extends OdeObject implements OdeFilterable {
    private static final long serialVersionUID = -7711340868799607662L;

    private OdeMsgMetadata metadata;
    private OdeMsgPayload payload;

    public OdeData() {
        super();
    }

    public OdeData(OdeMsgMetadata metadata, OdeMsgPayload payload) {
        super();
        this.metadata = metadata;
        this.payload = payload;
        this.metadata.setPayloadType(payload.getClass().getName());
    }

    public OdeMsgMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(OdeMsgMetadata metadata) {
        this.metadata = metadata;
    }

    public OdeMsgPayload getPayload() {
        return payload;
    }

    public void setPayload(OdeMsgPayload payload) {
        this.payload = payload;
    }


}
