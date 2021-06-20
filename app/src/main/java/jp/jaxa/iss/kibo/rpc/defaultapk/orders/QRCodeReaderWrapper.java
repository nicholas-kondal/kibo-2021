package jp.jaxa.iss.kibo.rpc.defaultapk.orders;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.qrcode.QRCodeReader;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;

public class QRCodeReaderWrapper {


    private final KiboRpcApi api;
    private final ImageHelper imageHelper;
    private final int loopMax;
    private final double flashlightOriginalBrightnessForScan;
    private final double flashlightFinalBrightnessForScan;

    private final QRCodeReader qrCodeReader;

    QRCodeReaderWrapper(KiboRpcApi api, ImageHelper imageHelper, int loopMax, double flashLightOriginalBrightnessForScan, double flashlightFinalBrightnessForScan) {

        this.api = api;
        this.imageHelper = imageHelper;
        this.loopMax = loopMax;
        this.flashlightOriginalBrightnessForScan = flashLightOriginalBrightnessForScan;
        this.flashlightFinalBrightnessForScan = flashlightFinalBrightnessForScan;

        this.qrCodeReader = new QRCodeReader();
    }

    /**
     * readQR is used to read a QR code
     * @return String content of the QR code
     */
    String readQR() { // TODO... Comment
        String contents;

        for (int count = 0; count < loopMax; count++) {

            // Gradually increasing brightness of flashlight
            api.flashlightControlFront(getCalculatedBrightness(loopMax, count));

            // Getting image from nav cam as bitmap
            BinaryBitmap bitmap = this.imageHelper.getBinaryBitmapFromMatImage(api.getMatNavCam(), api.getNavCamIntrinsics());

            // If read successful then return
            if ((contents = readQRCodeFromBitmap(bitmap)) != null) {
                api.flashlightControlFront((float)0); // Turning flashlight off?
                return contents;
            }
        }
        api.flashlightControlFront((float)0); // Turning flashlight off?
        throw new RobotOrderException("Unable to retrieve QR code");
    }

    private float getCalculatedBrightness(int loopMax, int count) {
        double maxCount = (double)(loopMax-1);

        // Camera is expected to go from dark to light so here we find the positive difference between the two
        double totalChangeInBrightness = flashlightFinalBrightnessForScan - flashlightOriginalBrightnessForScan;
        // Originally apply none of the change (count = 0), then finally apply all of the change (count = maxCount)
        double percentOfChangeToApply =  count/maxCount;

        // Return a float value equal to the initial brightness plus some percentage of the change
        return (float)(this.flashlightOriginalBrightnessForScan+(totalChangeInBrightness*percentOfChangeToApply));
    }

    private String readQRCodeFromBitmap(BinaryBitmap bitmap) {
        try {
            qrCodeReader.reset();
            return qrCodeReader.decode(bitmap).getText();
        } catch (NotFoundException | FormatException | ChecksumException e) { // TODO... Do something with error messages
            return null;
        }
    }


}
