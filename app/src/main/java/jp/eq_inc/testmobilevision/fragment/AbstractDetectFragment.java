package jp.eq_inc.testmobilevision.fragment;

import android.Manifest;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

import java.util.List;

import jp.eq_inc.testmobilevision.R;
import jp.eq_inc.testmobilevision.adapter.AbstractFaceDetectCursorAdapter;

abstract public class AbstractDetectFragment extends Fragment {
    protected OnFragmentInteractionListener mListener;
    protected AbstractFaceDetectCursorAdapter mItemAdapter;

    abstract public void changeCommonParams();

    public AbstractDetectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mItemAdapter != null) {
            mItemAdapter.updateAsync();
        }
    }

    public String[] getRuntimePermissions() {
        return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    }

    protected void changeCanvasPosition(Canvas imageCanvas, int frameRotation) {
        int imageWidth = imageCanvas.getWidth();
        int imageHeight = imageCanvas.getHeight();

        if (frameRotation != Frame.ROTATION_0) {
            switch (frameRotation) {
                case Frame.ROTATION_90:
                    imageCanvas.rotate(270, imageWidth / 2, imageHeight / 2);
                    imageCanvas.translate(-(imageHeight - imageWidth) / 2, (imageHeight - imageWidth) / 2);
                    break;
                case Frame.ROTATION_180:
                    imageCanvas.rotate(180, imageWidth / 2, imageHeight / 2);
                    break;
                case Frame.ROTATION_270:
                    imageCanvas.rotate(90, imageWidth / 2, imageHeight / 2);
                    imageCanvas.translate(-(imageHeight - imageWidth) / 2, (imageHeight - imageWidth) / 2);
                    break;
            }
        }
    }

    protected void drawQuadLine(Canvas imageCanvas, String description, float posX, float posY, float width, float height, Paint linePaint, int frameRotation) {
        int imageWidth = imageCanvas.getWidth();
        int imageHeight = imageCanvas.getHeight();
        float lineWidth = linePaint.getStrokeWidth();
        if (posX < 0) {
            posX = 0;
        }
        if (posY < 0) {
            posY = 0;
        }
        PointF faceRightBottomPointF = new PointF(posX + width, posY + height);
        if (frameRotation == Frame.ROTATION_0 || frameRotation == Frame.ROTATION_180) {
            if (faceRightBottomPointF.x > imageWidth) {
                faceRightBottomPointF.x = imageWidth;
            }
            if (faceRightBottomPointF.y > imageHeight) {
                faceRightBottomPointF.y = imageHeight;
            }
        } else if (frameRotation == Frame.ROTATION_90 || frameRotation == Frame.ROTATION_270) {
            if (faceRightBottomPointF.x > imageHeight) {
                faceRightBottomPointF.x = imageHeight;
            }
            if (faceRightBottomPointF.y > imageWidth) {
                faceRightBottomPointF.y = imageWidth;
            }
        }

        Path clipPath = new Path();
        imageCanvas.save();
        clipPath.addRect(posX + lineWidth, posY + lineWidth, faceRightBottomPointF.x - lineWidth, faceRightBottomPointF.y - lineWidth, Path.Direction.CW);
        imageCanvas.clipPath(clipPath, Region.Op.DIFFERENCE);
        imageCanvas.drawRect(posX, posY, faceRightBottomPointF.x, faceRightBottomPointF.y, linePaint);
        imageCanvas.restore();

        // 説明を表示
        if ((description != null) && (description.length() > 0)) {
            Paint idPaint = new Paint();
            idPaint.setColor(Color.BLACK);
            idPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.face_id_width));
            imageCanvas.drawText(description, posX, posY, idPaint);
        }
    }

    protected String getLandmarkTypeString(int type) {
        String ret = null;

        switch (type) {
            case Landmark.BOTTOM_MOUTH:
                ret = "bottom mouth";
                break;
            case Landmark.LEFT_CHEEK:
                ret = "left cheek";
                break;
            case Landmark.LEFT_EAR:
                ret = "left ear";
                break;
            case Landmark.LEFT_EAR_TIP:
                ret = "left ear tip";
                break;
            case Landmark.LEFT_EYE:
                ret = "left eye";
                break;
            case Landmark.LEFT_MOUTH:
                ret = "left mouth";
                break;
            case Landmark.NOSE_BASE:
                ret = "nose base";
                break;
            case Landmark.RIGHT_CHEEK:
                ret = "right cheek";
                break;
            case Landmark.RIGHT_EAR:
                ret = "right ear";
                break;
            case Landmark.RIGHT_EAR_TIP:
                ret = "right ear tip";
                break;
            case Landmark.RIGHT_EYE:
                ret = "right eye";
                break;
            case Landmark.RIGHT_MOUTH:
                ret = "right mouth";
                break;
        }

        return ret;
    }

    protected int getAdapterPosition(RecyclerView.ViewHolder holder) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 ? holder.getAdapterPosition() : holder.getPosition();
    }

    protected StringBuilder logOutputFace(StringBuilder builder, int indent, Face castedItem) {
        if (builder == null) {
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            indentBuilder.append(" ");
        }

        PointF position = castedItem.getPosition();
        builder.append(indentBuilder.toString()).append("ID: ").append(castedItem.getId()).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("face rect: ").append(position.x).append(", ").append(position.y).append(", ").append(position.x + castedItem.getWidth()).append(", ").append(position.y + castedItem.getHeight()).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("left eye is opend about ").append(castedItem.getIsLeftEyeOpenProbability()).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("right eye is opend about ").append(castedItem.getIsRightEyeOpenProbability()).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("smiling: ").append(castedItem.getIsSmilingProbability()).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("face angleYZ: ").append(castedItem.getEulerY()).append(", ").append(castedItem.getEulerZ()).append("\n");

        List<Landmark> landmarkList = castedItem.getLandmarks();
        if((landmarkList != null) && (landmarkList.size() > 0)){
            for(Landmark landmark : landmarkList){
                logOutputLandmark(builder, indent + 1, landmark);
            }
        }

        return builder;
    }

    protected StringBuilder logOutputLandmark(StringBuilder builder, int indent, Landmark landmark){
        if (builder == null) {
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder.toString()).append("landmark: ").append("\n");
        builder.append(indentBuilder.toString()).append(" ").append(getLandmarkTypeString(landmark.getType())).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append(landmark.getPosition()).append("\n");

        return builder;
    }

    protected StringBuilder logOutputBarcode(StringBuilder builder, int indent, Barcode castedItem){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder.toString()).append("Value: ").append(castedItem.displayValue).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("bounds: ").append(castedItem.getBoundingBox()).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("format: ").append(castedItem.format).append("\n");
        builder.append(indentBuilder.toString()).append(" ").append("value format: ");
        switch (castedItem.valueFormat) {
            case Barcode.CALENDAR_EVENT:
                builder.append(indentBuilder.toString()).append(" ").append("CALENDAR_EVENT").append("\n");
                logOutputCalendarEvent(builder, indent + 2, castedItem.calendarEvent);
                break;
            case Barcode.CONTACT_INFO:
                builder.append(indentBuilder.toString()).append(" ").append("CONTACT_INFO").append("\n");
                logOutputContactInfo(builder, indent + 2, castedItem.contactInfo);
                break;
            case Barcode.DRIVER_LICENSE:
                builder.append(indentBuilder.toString()).append(" ").append("DRIVER_LICENSE").append("\n");
                logOutputDriverLicense(builder, indent + 2, castedItem.driverLicense);
                break;
            case Barcode.EMAIL:
                builder.append(indentBuilder.toString()).append(" ").append("EMAIL").append("\n");
                logOutputEmail(builder, indent + 2, castedItem.email);
                break;
            case Barcode.GEO:
                builder.append(indentBuilder.toString()).append(" ").append("GEO").append("\n");
                logOutputGeo(builder, indent + 2, castedItem.geoPoint);
                break;
            case Barcode.ISBN:
                builder.append(indentBuilder.toString()).append(" ").append("ISBN").append("\n");
                // 処理なし
                break;
            case Barcode.PHONE:
                builder.append(indentBuilder.toString()).append(" ").append("PHONE").append("\n");
                logOutputPhone(builder, indent + 2, castedItem.phone);
                break;
            case Barcode.PRODUCT:
                builder.append(indentBuilder.toString()).append(" ").append("PRODUCT").append("\n");
                // 処理なし
                break;
            case Barcode.SMS:
                builder.append(indentBuilder.toString()).append(" ").append("SMS").append("\n");
                logOutputSms(builder, indent + 2, castedItem.sms);
                break;
            case Barcode.TEXT:
                builder.append(indentBuilder.toString()).append(" ").append("TEXT").append("\n");
                // 処理なし
                break;
            case Barcode.URL:
                builder.append(indentBuilder.toString()).append(" ").append("URL").append("\n");
                logOutputUrlBookmark(builder, indent + 2, castedItem.url);
                break;
            case Barcode.WIFI:
                builder.append(indentBuilder.toString()).append(" ").append("WIFI").append("\n");
                logOutputWifi(builder, indent + 2, castedItem.wifi);
                break;
            default:
                builder.append("\n");
        }

        return builder;
    }

    protected StringBuilder logOutputCalendarEvent(StringBuilder builder, int indent, Barcode.CalendarEvent calendarEvent) {
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("summary: ").append(calendarEvent.summary).append("\n");
        builder.append(indentBuilder).append("description: ").append(calendarEvent.description).append("\n");
        builder.append(indentBuilder).append("location: ").append(calendarEvent.location).append("\n");
        builder.append(indentBuilder).append("organizer: ").append(calendarEvent.organizer).append("\n");
        builder.append(indentBuilder).append("status: ").append(calendarEvent.status).append("\n");
        builder.append(indentBuilder).append("start: ").append(calendarEvent.start.year).append("/").append(calendarEvent.start.month).append("/").append(calendarEvent.start.day).append(" ").append(calendarEvent.start.hours).append(":").append(calendarEvent.start.minutes).append(":").append(calendarEvent.start.seconds).append(calendarEvent.start.isUtc ? " UTC" : "").append("\n");
        builder.append(indentBuilder).append("end: ").append(calendarEvent.end.year).append("/").append(calendarEvent.end.month).append("/").append(calendarEvent.end.day).append(" ").append(calendarEvent.end.hours).append(":").append(calendarEvent.end.minutes).append(":").append(calendarEvent.end.seconds).append(calendarEvent.end.isUtc ? " UTC" : "").append("\n");

        return builder;
    }

    protected StringBuilder logOutputContactInfo(StringBuilder builder, int indent, Barcode.ContactInfo contactInfo){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("title: ").append(contactInfo.title).append("\n");
        if(contactInfo.name != null){
            builder.append(indentBuilder).append("name: ").append("\n");
            builder.append(indentBuilder).append(" ").append("formattedName: ").append(contactInfo.name.formattedName).append("\n");
            builder.append(indentBuilder).append(" ").append("first: ").append(contactInfo.name.first).append("\n");
            builder.append(indentBuilder).append(" ").append("middle: ").append(contactInfo.name.middle).append("\n");
            builder.append(indentBuilder).append(" ").append("last: ").append(contactInfo.name.last).append("\n");
            builder.append(indentBuilder).append(" ").append("prefix: ").append(contactInfo.name.prefix).append("\n");
            builder.append(indentBuilder).append(" ").append("suffix: ").append(contactInfo.name.suffix).append("\n");
            builder.append(indentBuilder).append(" ").append("pronunciation: ").append(contactInfo.name.pronunciation).append("\n");
        }
        builder.append(indentBuilder).append("organization: ").append(contactInfo.organization).append("\n");
        if ((contactInfo.addresses != null) && (contactInfo.addresses.length > 0)) {
            for(Barcode.Address address : contactInfo.addresses){
                logOutputAddress(builder, indent, address);
            }
        }
        if((contactInfo.emails != null) && (contactInfo.emails.length > 0)){
            for(Barcode.Email email : contactInfo.emails){
                logOutputEmail(builder, indent, email);
            }
        }
        if((contactInfo.phones != null) && (contactInfo.phones.length > 0)){
            for(Barcode.Phone phone : contactInfo.phones){
                logOutputPhone(builder, indent, phone);
            }
        }
        if((contactInfo.urls != null) && (contactInfo.urls.length > 0)){
            for(String url : contactInfo.urls) {
                builder.append("  ").append("url: ").append(url).append("\n");
            }
        }

        return builder;
    }

    protected StringBuilder logOutputAddress(StringBuilder builder, int indent, Barcode.Address address){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("address type: ").append(address.type).append("\n");
        if(address.addressLines != null && address.addressLines.length > 0){
            for(String addressLine : address.addressLines){
                builder.append(indentBuilder).append(" ").append("addressLine: ").append(addressLine).append("\n");
            }
        }

        return builder;
    }

    protected StringBuilder logOutputDriverLicense(StringBuilder builder, int indent, Barcode.DriverLicense driverLicense){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("licenseNumber: ").append(driverLicense.licenseNumber).append("\n");
        builder.append(indentBuilder).append("firstName: ").append(driverLicense.firstName).append("\n");
        builder.append(indentBuilder).append("middleName: ").append(driverLicense.middleName).append("\n");
        builder.append(indentBuilder).append("lastName: ").append(driverLicense.lastName).append("\n");
        builder.append(indentBuilder).append("gender: ").append(driverLicense.gender).append("\n");
        builder.append(indentBuilder).append("birthDate: ").append(driverLicense.birthDate).append("\n");
        builder.append(indentBuilder).append("addressZip: ").append(driverLicense.addressZip).append("\n");
        builder.append(indentBuilder).append("addressState: ").append(driverLicense.addressState).append("\n");
        builder.append(indentBuilder).append("addressCity: ").append(driverLicense.addressCity).append("\n");
        builder.append(indentBuilder).append("addressStreet: ").append(driverLicense.addressStreet).append("\n");
        builder.append(indentBuilder).append("expiryDate: ").append(driverLicense.expiryDate).append("\n");
        builder.append(indentBuilder).append("documentType: ").append(driverLicense.documentType).append("\n");
        builder.append(indentBuilder).append("issueDate: ").append(driverLicense.issueDate).append("\n");
        builder.append(indentBuilder).append("issuingCountry: ").append(driverLicense.issuingCountry).append("\n");

        return builder;
    }

    protected StringBuilder logOutputEmail(StringBuilder builder, int indent, Barcode.Email email){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("email type: ").append(email.type).append("\n");
        builder.append(indentBuilder).append(" ").append("address: ").append(email.address).append("\n");
        builder.append(indentBuilder).append(" ").append("subject: ").append(email.subject).append("\n");
        builder.append(indentBuilder).append(" ").append("body: ").append(email.body).append("\n");

        return builder;
    }

    protected StringBuilder logOutputGeo(StringBuilder builder, int indent, Barcode.GeoPoint geoPoint){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("latitude: ").append(geoPoint.lat).append("\n");
        builder.append(indentBuilder).append("longitude: ").append(geoPoint.lng).append("\n");

        return builder;
    }

    protected StringBuilder logOutputPhone(StringBuilder builder, int indent, Barcode.Phone phone){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("phone type: ").append(phone.type).append("\n");
        builder.append(indentBuilder).append(" ").append("phone number: ").append(phone.number).append("\n");

        return builder;
    }

    protected StringBuilder logOutputSms(StringBuilder builder, int indent, Barcode.Sms sms){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("phoneNumber: ").append(sms.phoneNumber).append("\n");
        builder.append(indentBuilder).append("message: ").append(sms.message).append("\n");

        return builder;
    }

    protected StringBuilder logOutputUrlBookmark(StringBuilder builder, int indent, Barcode.UrlBookmark urlBookmark){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("title: ").append(urlBookmark.title).append("\n");
        builder.append(indentBuilder).append("url: ").append(urlBookmark.url).append("\n");

        return builder;
    }

    protected StringBuilder logOutputWifi(StringBuilder builder, int indent, Barcode.WiFi wifi){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("ssid: ").append(wifi.ssid).append("\n");
        builder.append(indentBuilder).append("password: ").append(wifi.password).append("\n");
        switch (wifi.encryptionType){
            case Barcode.WiFi.OPEN:
                builder.append(indentBuilder).append("encryptionType: OPEN").append("\n");
                break;
            case Barcode.WiFi.WEP:
                builder.append(indentBuilder).append("encryptionType: WEP").append("\n");
                break;
            case Barcode.WiFi.WPA:
                builder.append(indentBuilder).append("encryptionType: WPA").append("\n");
                break;
        }

        return builder;
    }

    protected StringBuilder logOutputTextBlock(StringBuilder builder, int indent, TextBlock textBlock){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("Value: ").append(textBlock.getValue()).append("\n");
        builder.append(indentBuilder).append(" ").append("bounds: ").append(textBlock.getBoundingBox()).append("\n");
        builder.append(indentBuilder).append(" ").append("language: ").append(textBlock.getLanguage()).append("\n");

        List<? extends Text> textComponentList = textBlock.getComponents();
        if(textComponentList != null && textComponentList.size() > 0){
            for(Text textComponent : textComponentList){
                logOutputTextComponent(builder, indent + 1, textComponent);
            }
        }

        return builder;
    }

    protected StringBuilder logOutputTextComponent(StringBuilder builder, int indent, Text textComponent){
        if(builder == null){
            builder = new StringBuilder();
        }
        StringBuilder indentBuilder = new StringBuilder();
        for(int i=0; i<indent; i++){
            indentBuilder.append(" ");
        }

        builder.append(indentBuilder).append("Value: ").append(textComponent.getValue()).append("\n");
        builder.append(indentBuilder).append(" ").append("bounds: ").append(textComponent.getBoundingBox()).append("\n");

        List<? extends Text> textComponentList = textComponent.getComponents();
        if(textComponentList != null && textComponentList.size() > 0){
            for(Text childTextComponent : textComponentList){
                logOutputTextComponent(builder, indent + 1, childTextComponent);
            }
        }

        return builder;
    }
}
