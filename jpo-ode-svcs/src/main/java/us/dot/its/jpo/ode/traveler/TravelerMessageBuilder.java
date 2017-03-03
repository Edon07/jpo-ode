package us.dot.its.jpo.ode.traveler;


import com.oss.asn1.EncodeFailedException;
import com.oss.asn1.EncodeNotSupportedException;
import us.dot.its.jpo.ode.j2735.dsrc.*;
import us.dot.its.jpo.ode.j2735.dsrc.TravelerDataFrame.Content;
import us.dot.its.jpo.ode.j2735.dsrc.TravelerDataFrame.MsgId;
import us.dot.its.jpo.ode.j2735.itis.ITIScodesAndText;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;





/**
 * Created by anthonychen on 2/16/17.
 */
public class TravelerMessageBuilder {
    public TravelerInformation travelerInfo;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm a");
    private static final int MAX_MINUTES_DURATION = 32000; // DSRC spec


    private TravelerInformation buildTravelerInformation(TravelerInputData travInputData)
            throws ParseException, EncodeFailedException, EncodeNotSupportedException {


        travelerInfo = new TravelerInformation();
        travelerInfo.setMsgCnt(new MsgCount(travelerInfo.getDataFrames().getSize()));
        ByteBuffer buf = ByteBuffer.allocate(9).put((byte)0).putLong(travInputData.packetID);
        travelerInfo.setPacketID(new UniqueMSGID(buf.array()));
        travelerInfo.setUrlB(new URL_Base(travInputData.urlB));

        travelerInfo.setDataFrames(buildDataFrames(travInputData));

        return travelerInfo;
    }

    private TravelerDataFrameList buildDataFrames(TravelerInputData travInputData) throws ParseException {
        TravelerDataFrameList dataFrames = new TravelerDataFrameList();

        int len = travInputData.dataframes.length;
        for (int i =0 ; i <len; i++)
        {
            TravelerInputData.DataFrame inputDataFrame = travInputData.dataframes[i];
            TravelerDataFrame dataFrame = new TravelerDataFrame();

            // Part I, header
            dataFrame.setSspTimRights(new SSPindex(inputDataFrame.sspTimRights));
            dataFrame.setFrameType(TravelerInfoType.valueOf(inputDataFrame.infoType));
            dataFrame.setMsgId(getMessageId(inputDataFrame));
            dataFrame.setStartYear(new DYear(getStartYear(inputDataFrame)));
            dataFrame.setStartTime(new MinuteOfTheYear(getStartTime(inputDataFrame)));
//            dataFrame.setDuratonTime(new MinutesDuration(getDurationTime(inputDataFrame)));
            dataFrame.setPriority(new SignPrority(inputDataFrame.priority));

            // -- Part II, Applicable Regions of Use
            dataFrame.setSspLocationRights(new SSPindex(inputDataFrame.sspLocationRights));
//            dataFrame.setRegions(buildRegions(travInputData));

            // -- Part III, Content
            dataFrame.setSspMsgRights1(new SSPindex(inputDataFrame.sspTimRights));		// allowed message types
            dataFrame.setSspMsgRights2(new SSPindex(inputDataFrame.sspMsgContent));	    // allowed message content
            dataFrame.setContent(buildContent(inputDataFrame));
            dataFrame.setUrl(new URL_Short(inputDataFrame.url));

            dataFrames.add(dataFrame);
        }


        return dataFrames;
    }
    private Content buildContent(TravelerInputData.DataFrame dataFrame) {
        String contentType = dataFrame.content;
        String[] codes = dataFrame.items;
        Content content = new Content();
        if ("Advisory".equals(contentType)) {
            content.setAdvisory(buildAdvisory(codes));
        } else if ("Work Zone".equals(contentType)) {
            content.setWorkZone(buildWorkZone(codes));
        } else if ("Speed Limit".equals(contentType)) {
            content.setSpeedLimit(buildSpeedLimit(codes));
        } else if ("Exit Service".equals(contentType)) {
            content.setExitService(buildExitService(codes));
        } else {
            content.setGenericSign(buildGenericSignage(codes));
        }
        return content;
    }

    private ITIScodesAndText buildAdvisory(String[] codes) {
        ITIScodesAndText itisText = new ITIScodesAndText();
        for (String code: codes) {
            ITIScodesAndText.Sequence_ seq = new ITIScodesAndText.Sequence_();
            ITIScodesAndText.Sequence_.Item item = new ITIScodesAndText.Sequence_.Item();
            item.setItis(Long.parseLong(code));
            seq.setItem(item);
            itisText.add(seq);
        }
        return itisText;
    }

    private WorkZone buildWorkZone(String[] codes) {
        WorkZone wz = new WorkZone();
        for (String code: codes) {
            WorkZone.Sequence_ seq = new WorkZone.Sequence_();
            WorkZone.Sequence_.Item item = new WorkZone.Sequence_.Item();
            item.setItis(Long.parseLong(code));
            seq.setItem(item);
            wz.add(seq);
        }

        return wz;
    }

    private SpeedLimit buildSpeedLimit(String[] codes) {
        SpeedLimit sl = new SpeedLimit();
        for (String code: codes) {
            SpeedLimit.Sequence_ seq = new SpeedLimit.Sequence_();
            SpeedLimit.Sequence_.Item item = new SpeedLimit.Sequence_.Item();
            item.setItis(Long.parseLong(code));
            seq.setItem(item);
            sl.add(seq);
        }
        return sl;
    }

    private ExitService buildExitService(String[] codes) {
        ExitService es = new ExitService();
        for (String code: codes) {
            ExitService.Sequence_ seq = new ExitService.Sequence_();
            ExitService.Sequence_.Item item = new ExitService.Sequence_.Item();
            item.setItis(Long.parseLong(code));
            seq.setItem(item);
            es.add(seq);
        }
        return es;
    }

    private GenericSignage buildGenericSignage(String[] codes) {
        GenericSignage gs = new GenericSignage();
        for (String code: codes) {
            GenericSignage.Sequence_ seq = new GenericSignage.Sequence_();
            GenericSignage.Sequence_.Item item = new GenericSignage.Sequence_.Item();
            item.setItis(Long.parseLong(code));
            seq.setItem(item);
            gs.add(seq);
        }
        return gs;
    }

    //TODO not hardcode for a road sign an  dataframe anchor point
    private MsgId getMessageId(TravelerInputData.DataFrame dataFrame) {
        MsgId msgId = new MsgId();

        TravelerInputData.DataFrame.RoadSign roadSign = dataFrame.roadSign;
        if (roadSign != null) {
            msgId.setChosenFlag(MsgId.roadSignID_chosen);
            RoadSignID roadSignID = new RoadSignID();
//            roadSignID.setPosition(getAnchorPointPosition(dataFrame));
            roadSignID.setViewAngle(getHeadingSlice(dataFrame));
            roadSignID.setMutcdCode(MUTCDCode.valueOf(dataFrame.mutcd));
            msgId.setRoadSignID(roadSignID);
        } else {
            msgId.setChosenFlag(MsgId.furtherInfoID_chosen);
            msgId.setFurtherInfoID(new FurtherInfoID(new byte[] { 0x00,0x00 }));
        }
        return msgId;
    }

    private HeadingSlice getHeadingSlice(TravelerInputData.DataFrame dataFrame) {
        String[] heading = dataFrame.heading;
        if (heading == null || heading.length == 0) {
            return new HeadingSlice(new byte[] { 0x00,0x00 });
        } else {
            int[] nums = new int[heading.length];
            for (int i=0; i<heading.length; i++) {
                nums[i] = Integer.parseInt(heading[i], 16);
            }
            short result = 0;
            for (int i=0; i<nums.length; i++) {
                result|= nums[i];
            }
            return new HeadingSlice(ByteBuffer.allocate(2).putShort(result).array());
        }
    }
//    private static Position3D getAnchorPointPosition(TravelerInputData.DataFrame anchorPoint) {
//        assert(anchorPoint != null);
//        final int elev = anchorPoint.getReferenceElevation();
//        Position3D anchorPos = new Position3D(
//                new Latitude(J2735Util.convertGeoCoordinateToInt(anchorPoint.referenceLat)),
//                new Longitude(J2735Util.convertGeoCoordinateToInt(anchorPoint.referenceLon)));
//        anchorPos.setElevation(new Elevation(elev));
//        return anchorPos;
//    }
//
//    private static Position3D build3DPosition(TravelerInputData.DataFrame anchorPoint) {
//        assert(anchorPoint != null);
//        final int elev = anchorPoint.getReferenceElevation();
//        Position3D anchorPos = new Position3D(
//                new Latitude(J2735Util.convertGeoCoordinateToInt(anchorPoint.referenceLat)),
//                new Longitude(J2735Util.convertGeoCoordinateToInt(anchorPoint.referenceLon)));
//        anchorPos.setElevation(new Elevation(elev));
//        return anchorPos;
//    }
//
//    private Regions buildRegions(TravelerInputData.DataFrame travInputData) {
//        Regions regions = new Regions();
//        for (Region inputRegion: travInputData.regions) {
//            GeographicalPath geoPath = new GeographicalPath();
//            ValidRegion validRegion = new ValidRegion();
//            validRegion.setDirection(getHeadingSlice(travInputData));
//            if (inputRegion.extent != -1) {
//                validRegion.setExtent(Extent.valueOf(inputRegion.extent));
//            }
//            validRegion.setArea(buildArea(travInputData, inputRegion));
//            Description description = new Description();
//            description.setOldRegion(validRegion);
//            geoPath.setDescription(description);
//            regions.add(geoPath);
//        }
//        return regions;
//    }
//    private Area buildArea(TravelerInputData travInputData, Region inputRegion) {
//        Area area = new Area();
//        Position3D anchorPos = getAnchorPointPosition(travInputData.anchorPoint);
//        if (inputRegion.regionType.equals("lane")) {
//            ShapePointSet sps = new ShapePointSet();
//            sps.setAnchor(anchorPos);
//            sps.setLaneWidth(new LaneWidth(travInputData.anchorPoint.masterLaneWidth));
//            sps.setDirectionality(DirectionOfUse.valueOf(travInputData.anchorPoint.direction));
//            sps.setNodeList(buildNodeList(inputRegion.laneNodes, travInputData.anchorPoint.referenceElevation));
//            area.setShapePointSet(sps);
//        } else if (inputRegion.regionType.equals("region")) {
//            RegionPointSet rps = new RegionPointSet();
//            rps.setAnchor(anchorPos);
//            RegionList regionList = new RegionList();
//            GeoPoint refPoint = inputRegion.refPoint;
//            for (int i=0; i < inputRegion.laneNodes.length; i++) {
//                GeoPoint nextPoint = new GeoPoint(inputRegion.laneNodes[i].nodeLat, inputRegion.laneNodes[i].nodeLong);
//                regionList.add(buildRegionOffset(refPoint, nextPoint));
//                refPoint = nextPoint;
//            }
//            rps.setNodeList(regionList);
//            area.setRegionPointSet(rps);
//        }
//        return area;
//    }

    private long getStartTime(TravelerInputData.DataFrame dataFrame) throws ParseException {
        Date startDate = sdf.parse(dataFrame.startTime);
        String startOfYearTime = "01/01/" + getStartYear(dataFrame) + " 12:00 AM";
        Date startOfYearDate = sdf.parse(startOfYearTime);
        long minutes = ((startDate.getTime() - startOfYearDate.getTime()) / 60000);
        return minutes;
    }

    private int getStartYear(TravelerInputData.DataFrame dataFrame) throws ParseException {
        Date startDate = sdf.parse(dataFrame.startTime);
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        return cal.get(Calendar.YEAR);
    }

//    private int getDurationTime(TravelerInputData.DataFrame dataFrame) throws ParseException {
//        Date startDate = sdf.parse(dataFrame.startTime);
//        Date endDate = sdf.parse(dataFrame.endTime);
//
//        long diff = endDate.getTime() - startDate.getTime();
//        int durationInMinutes = (int) diff / 1000 / 60;
//        if (durationInMinutes > MAX_MINUTES_DURATION)
//            durationInMinutes = MAX_MINUTES_DURATION;
//        return durationInMinutes;
//    }




//    private RegionOffsets buildRegionOffset(GeoPoint refPoint, GeoPoint nextPoint) {
//        short xOffset = nextPoint.getLonOffsetInMeters(refPoint);
//        short yOffset = nextPoint.getLatOffsetInMeters(refPoint);
//        RegionOffsets offsets = new RegionOffsets(new OffsetLL_B16(xOffset), new OffsetLL_B16(yOffset));
//        return offsets;
//    }

}
