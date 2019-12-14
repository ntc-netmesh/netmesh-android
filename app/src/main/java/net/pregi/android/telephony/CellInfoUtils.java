package net.pregi.android.telephony;

import android.telephony.TelephonyManager;
import android.util.SparseArray;

public class CellInfoUtils {
    public static final int UNAVAILABLE = 0x7FFFFFFF;

    private static final SparseArray<SparseArray<String>> OPERATOR_NAME_BY_MCC_MNC = new SparseArray<>();
    static {
        // Source: https://www.mcc-mnc.com/
        // Specifics: https://en.wikipedia.org/wiki/Mobile_Network_Codes_in_ITU_region_5xx_(Oceania)#Philippines_-_PH
        //  https://cellidfinder.com/mcc-mnc
        SparseArray<String> opNamePh = new SparseArray<>();
        opNamePh.put(0, "Fixed Line");
        opNamePh.put(1, "Islacom (Globe Telecom)");
        opNamePh.put(2, "Globe Telecom");
        opNamePh.put(3, "SMART (PLDT)");
        opNamePh.put(5, "Sun Cellular (Digitel)");
        opNamePh.put(11, "ACeS (PLDT)");
        opNamePh.put(18, "Cure (PLDT)");
        opNamePh.put(24, "ABS-CBN Mobile");
        opNamePh.put(88, "Next Mobile");
        OPERATOR_NAME_BY_MCC_MNC.put(515, opNamePh);
    }

    /** <p>Get the brand name of a given MCC + MNC combination.</p>
     *
     * @param mcc
     * @param mnc
     * @return null if the MCC and MNC aren't mapped.
     */
    public static String getMobileName(int mcc, int mnc) {
        SparseArray<String> namesInCountry = OPERATOR_NAME_BY_MCC_MNC.get(mcc);
        if (namesInCountry != null) {
            return namesInCountry.get(mnc);
        } else {
            return null;
        }
    }

    public CharSequence toMobileNetworkCodeString(int mcc, int mnc) {
        if (mcc == UNAVAILABLE) {
            if (mnc == UNAVAILABLE) {
                return messageUnavailable;
            } else {
                return String.format("???-%02d", mnc);
            }
        } else {
            if (mnc == UNAVAILABLE) {
                return String.format("%03d-??", mcc);
            } else {
                String mccMncName = getMobileName(mcc, mnc);
                if (mccMncName != null) {
                    return String.format("%03d-%02d (%s)", mcc, mnc, mccMncName);
                } else {
                    return String.format("%03d-%02d", mcc, mnc);
                }
            }
        }
    }

    public String toNetworkTypeString(int value) {
        switch(value) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS (GSM)";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE (GSM)";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSPDA (UTMS)";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA (UTMS)";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA (UTMS)";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "EHRPD";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO B";
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "GSM";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "IDEN";
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "IWLAN";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "TD-SCDMA";
            default:
                return null;
        }
    }

    public CharSequence rssiToDbmString(int networkType, int valueCode) {
        switch (networkType) {
            case 99: // UNKNOWN_RSSI
                return null;
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
                if (valueCode<=0) {
                    return "-113 or less";
                } else if (valueCode>=31) {
                    return "-51 or greater";
                } else {
                    return Integer.toString(-113 + 2 * valueCode);
                }
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
                if (valueCode<=0) {
                    return "less than "+(-115-valueCode);
                } else if (valueCode<=90) {
                    return "between "+(-116+valueCode)+" and "+(-115+valueCode);
                } else {
                    return "at least "+(valueCode-116);
                }
            default:
                return null;
        }
    }

    public CharSequence toIdOrUnavailable(int rangeMax, int value) {
        if (0<=value && value<=rangeMax) {
            return Integer.toString(value);
        } else {
            return messageUnavailable;
        }
    }
    public CharSequence toIntOrUnavailable(int value) {
        if (value != UNAVAILABLE) {
            return Integer.toString(value);
        } else {
            return messageUnavailable;
        }
    }

    public CharSequence toBaseStationString(int baseStationId) {
        return toIdOrUnavailable(65535, baseStationId);
    }
    public CharSequence toNetworkIdString(int networkId) {
        return toIdOrUnavailable(65535, networkId);
    }
    public CharSequence toSystemIdString(int systemId) {
        return toIdOrUnavailable(32767, systemId);
    }
    public CharSequence toCellIdString(int cellId) {
        return toIdOrUnavailable(65535, cellId);
    }
    public CharSequence toLocationAreaCodeString(int lac) {
        return toIdOrUnavailable(65535, lac);
    }


    private static final char DEGREE_SIGN = '\u00B0';

    public CharSequence toGeolocationString(int latitude, int longitude) {
        String lat = null;
        String lon = null;
        if (-1296000<=latitude && latitude<=1296000) {
            boolean south = false;
            if (latitude<0) {
                latitude = -latitude;
                south = true;
            }

            int degrees = latitude/14400;
            int minutes = (latitude/240)%60;
            float seconds = latitude*0.25f-minutes*60-degrees*3600;

            lat = String.format("%d"+DEGREE_SIGN+" %d' %.2f\" %s", degrees, minutes, seconds, south ? "S" : "N");
        }
        if (-2592000<=longitude && longitude<=2592000) {
            boolean west = false;
            if (longitude<0) {
                longitude = -longitude;
                west = true;
            }

            int degrees = longitude/14400;
            int minutes = (longitude/240)%60;
            float seconds = longitude*0.25f-minutes*60-degrees*3600;

            lon = String.format("%d"+DEGREE_SIGN+" %d' %.2f\" %s", degrees, minutes, seconds, west ? "S" : "N");
        }
        if (lat == null && lon == null) {
            return messageUnavailable;
        } else {
            if (lat == null) {
                return "???, "+lon;
            } else if (lon == null) {
                return lat+", ???";
            } else {
                return lat+", "+lon;
            }
        }
    }

    private CharSequence messageUnavailable = "";
    public CellInfoUtils setMessageUnavailable(CharSequence value) {
        messageUnavailable = value;
        return this;
    }

    public CellInfoUtils() {

    }
}
