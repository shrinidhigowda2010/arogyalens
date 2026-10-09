package com.arogyalens.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Built-in translations of common phrases so key UI text works without AI. */
@Component
public class OfflineLanguagePack {

    private static final Map<String, Map<String, String>> PACK = new LinkedHashMap<>();

    static {
        put(
                "within_range",
                "Within provided reference range",
                "दिए गए संदर्भ सीमा के अंदर",
                "ನೀಡಿರುವ ಉಲ್ಲೇಖ ವ್ಯಾಪ್ತಿಯೊಳಗೆ",
                "வழங்கப்பட்ட குறிப்பு வரம்பிற்குள்",
                "ఇచ్చిన సూచన పరిధిలో",
                "दिलेल्या संदर्भ मर्यादेत",
                "প্রদত্ত রেফারেন্স সীমার মধ্যে");
        put(
                "needs_discussion",
                "Worth discussing with a healthcare professional",
                "स्वास्थ्य विशेषज्ञ से चर्चा करने योग्य",
                "ಆರೋಗ್ಯ ತಜ್ಞರೊಂದಿಗೆ ಚರ್ಚಿಸಬೇಕಾದುದು",
                "மருத்துவ நிபுணரிடம் பேச வேண்டியது",
                "వైద్య నిపుణుడితో చర్చించదగినది",
                "आरोग्य व्यावसायिकांशी चर्चा करण्यासारखे",
                "স্বাস্থ্য পেশাদারের সাথে আলোচনার যোগ্য");
        put(
                "important_attention",
                "Requires professional evaluation",
                "पेशेवर मूल्यांकन आवश्यक",
                "ವೃತ್ತಿಪರ ಮೌಲ್ಯಮಾಪನ ಅಗತ್ಯ",
                "தொழில்முறை மதிப்பீடு தேவை",
                "వృత్తిపరమైన అంచనా అవసరం",
                "व्यावसायिक मूल्यमापन आवश्यक",
                "পেশাদার মূল্যায়ন প্রয়োজন");
        put(
                "your_results",
                "Your results",
                "आपके परिणाम",
                "ನಿಮ್ಮ ಫಲಿತಾಂಶಗಳು",
                "உங்கள் முடிவுகள்",
                "మీ ఫలితాలు",
                "तुमचे निकाल",
                "আপনার ফলাফল");
        put(
                "questions_doctor",
                "Questions for your doctor",
                "अपने डॉक्टर के लिए प्रश्न",
                "ನಿಮ್ಮ ವೈದ್ಯರಿಗೆ ಪ್ರಶ್ನೆಗಳು",
                "உங்கள் மருத்துவருக்கான கேள்விகள்",
                "మీ వైద్యుని కోసం ప్రశ్నలు",
                "तुमच्या डॉक्टरांसाठी प्रश्न",
                "আপনার ডাক্তারের জন্য প্রশ্ন");
        put(
                "upload_title",
                "Upload your document",
                "अपना दस्तावेज़ अपलोड करें",
                "ನಿಮ್ಮ ದಾಖಲೆಯನ್ನು ಅಪ್‌ಲೋಡ್ ಮಾಡಿ",
                "உங்கள் ஆவணத்தை பதிவேற்றவும்",
                "మీ పత్రాన్ని అప్‌లోడ్ చేయండి",
                "तुमचा दस्तऐवज अपलोड करा",
                "আপনার নথি আপলোড করুন");
        put(
                "analyze",
                "Analyze document",
                "दस्तावेज़ विश्लेषण करें",
                "ದಾಖಲೆ ವಿಶ್ಲೇಷಿಸಿ",
                "ஆவணத்தை பகுப்பாய்வு செய்",
                "పత్రాన్ని విశ్లేషించండి",
                "दस्तऐवजाचे विश्लेषण करा",
                "নথি বিশ্লেষণ করুন");
        put(
                "hba1c",
                "HbA1c is a blood test that estimates average blood sugar over the previous few months.",
                "HbA1c एक रक्त जांच है जो पिछले कुछ महीनों के औसत रक्त शर्करा का अनुमान लगाती है।",
                "HbA1c ರಕ್ತದಲ್ಲಿನ ಸರಾಸರಿ ಸಕ್ಕರೆ ಮಟ್ಟವನ್ನು ಕಳೆದ ಕೆಲವು ತಿಂಗಳುಗಳಲ್ಲಿ ಅಂದಾಜು ಮಾಡುವ ಪರೀಕ್ಷೆ.",
                "HbA1c என்பது கடந்த சில மாதங்களின் சராசரி இரத்த சர்க்கரையை மதிப்பிடும் இரத்தப் பரிசோதனை.",
                "HbA1c అనేది గత కొన్ని నెలల సగటు రక్త చక్కెరను అంచనా వేసే రక్త పరీక్ష.",
                "HbA1c ही गेल्या काही महिन्यांच्या सरासरी रक्तसाखरेचा अंदाज देणारी रक्ततपासणी आहे.",
                "HbA1c একটি রক্ত পরীক্ষা যা গত কয়েক মাসের গড় রক্তে শর্করার আনুমানিক মান দেয়।");
    }

    private static void put(
            String key,
            String en,
            String hi,
            String kn,
            String ta,
            String te,
            String mr,
            String bn) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("en", en);
        map.put("hi", hi);
        map.put("kn", kn);
        map.put("ta", ta);
        map.put("te", te);
        map.put("mr", mr);
        map.put("bn", bn);
        PACK.put(key, map);
    }

    public String get(String key, String language) {
        Map<String, String> map = PACK.get(key);
        if (map == null) {
            return key;
        }
        String lang = language == null ? "en" : language.toLowerCase(Locale.ROOT);
        return map.getOrDefault(lang, map.get("en"));
    }

    public Map<String, String> all(String key) {
        return PACK.getOrDefault(key, Map.of("en", key));
    }

    public String translateMedicalBlurb(String text, String language) {
        if (text == null) {
            return "";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("hba1c")) {
            return get("hba1c", language);
        }
        return text;
    }
}
