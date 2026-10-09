import type { AppLanguage } from '../types'

type Dict = Record<string, string>

const en: Dict = {
  tagline: 'See. Understand. Hear. Act.',
  positioning: 'The AI accessibility layer for healthcare',
  hero: 'Healthcare, understood by everyone.',
  heroBody:
    'Upload a report, prescription, medicine package, or discharge summary. We mask personal details, explain medical language, and help you listen in your language.',
  scanReport: 'Scan a report',
  scanMedical: 'Scan Medical Report',
  scanMedicalDesc: 'Understand lab values in plain language, in your language.',
  scanMedicine: 'Scan Medicine',
  scanMedicineDesc: 'Learn what a medicine is generally used for — not personal dosing advice.',
  scanRx: 'Scan Prescription',
  scanRxDesc: 'See morning, afternoon, and night timing at a glance.',
  scanDischarge: 'Understand Discharge Summary',
  scanDischargeDesc: 'Follow-up, medicines, and warning signs — clearly organized.',
  ask: 'Ask ArogyaLens',
  askDesc: 'Chat grounded in a recent scan session.',
  recent: 'Recent sessions',
  footerQuote:
    "Healthcare information shouldn't be difficult just because it is written in the wrong language.",
  footerMission: "We don't replace the doctor. We make healthcare easier to understand.",
  uploadTitle: 'Upload your document',
  uploadHint:
    'Drag and drop, or choose a JPG, PNG, WEBP, or PDF. Personal details are masked before analysis.',
  chooseFile: 'Choose file',
  selected: 'Selected',
  analyze: 'Analyze document',
  needFile: 'Please choose a file to analyze.',
  yourResults: 'Your results',
  sources: 'Sources',
  multilingual: 'Explanation in your language',
  noResults: 'No results yet.',
  uploadFirst: 'Upload a document to begin.',
  backHome: 'Home',
  accessibility: 'Accessibility',
  listening: 'Listen',
  explain12: "Explain like I'm 12",
  skipToContent: 'Skip to main content',
  navHome: 'Home',
  askLabel: 'Ask a health question',
  askPlaceholder: 'Ask any health question…',
  askButton: 'Ask',
  askHint: 'Type or speak in your language. Answers are general information, not a diagnosis.',
  micStart: 'Speak your question',
  micStop: 'Stop listening',
  micListening: 'Listening… speak now',
  micUnsupported: 'Voice input works in Chrome or Edge; you can type here instead.',
  answerHeading: 'Answer',
  thinking: 'Finding an answer…',
  listen: 'Listen',
  stop: 'Stop',
  answerLanguage: 'Answer language',
  doctors: 'Find a doctor',
  doctorsDesc: 'Find the right kind of specialist near you, or consult online.',
  concernLabel: 'Symptoms, condition or test result',
  concernPlaceholder: 'e.g. high blood sugar, skin rash, chest pain',
  suggestSpecialty: 'Suggest specialist',
  specialtyLabel: 'Specialist to look for',
  locationLabel: 'City or PIN code',
  useMyLocation: 'Use my location',
  locating: 'Getting your location…',
  findDoctors: 'Find doctors',
  searching: 'Searching…',
  call: 'Call',
  directions: 'Directions',
  openNow: 'Open now',
  closedNow: 'Closed now',
  emergencyTitle: 'Emergency? Call 108 (ambulance) or 112 now.',
  emergencyBody:
    'Do not wait for an online answer if someone has chest pain, trouble breathing, stroke signs, heavy bleeding or is unconscious.',
  sourcesHeading: 'Trusted sources',
  history: 'My history',
  historyDesc:
    'Keep a private list of your scans and questions on this device. Names, phone numbers and IDs are masked before anything is saved.',
  historyToggle: 'Save my scans and questions',
  historyEmpty: 'Nothing saved yet.',
  historyOff: 'History is off. Turn it on to save new scans and questions.',
  historyDelete: 'Delete',
  historyDeleteAll: 'Delete all history',
  historyConfirm: 'Delete all saved history? This cannot be undone.',
  historyDeleted: 'Deleted.',
  historyScan: 'Scan',
  historyChat: 'Question',
  loading: 'Loading…',
}

const hi: Dict = {
  ...en,
  askPlaceholder: 'कोई भी स्वास्थ्य प्रश्न पूछें…',
  askButton: 'पूछें',
  doctors: 'डॉक्टर खोजें',
  emergencyTitle: 'आपातकाल? अभी 108 (एम्बुलेंस) या 112 पर कॉल करें।',
  positioning:
    '\u0938\u094d\u0935\u093e\u0938\u094d\u0925\u094d\u092f \u0938\u0947\u0935\u093e \u0915\u0947 \u0932\u093f\u090f AI \u0938\u0941\u0932\u092d\u0924\u093e \u092a\u0930\u0924',
  hero: '\u0938\u094d\u0935\u093e\u0938\u094d\u0925\u094d\u092f \u0938\u0947\u0935\u093e, \u091c\u093f\u0938\u0947 \u0939\u0930 \u0915\u094b\u0908 \u0938\u092e\u091d \u0938\u0915\u0947\u0964',
  scanReport:
    '\u0930\u093f\u092a\u094b\u0930\u094d\u091f \u0938\u094d\u0915\u0948\u0928 \u0915\u0930\u0947\u0902',
  scanMedical:
    '\u092e\u0947\u0921\u093f\u0915\u0932 \u0930\u093f\u092a\u094b\u0930\u094d\u091f \u0938\u094d\u0915\u0948\u0928 \u0915\u0930\u0947\u0902',
  scanMedicine: '\u0926\u0935\u093e \u0938\u094d\u0915\u0948\u0928 \u0915\u0930\u0947\u0902',
  scanRx:
    '\u0928\u0941\u0938\u094d\u0916\u093e \u0938\u094d\u0915\u0948\u0928 \u0915\u0930\u0947\u0902',
  scanDischarge:
    '\u0921\u093f\u0938\u094d\u091a\u093e\u0930\u094d\u091c \u0938\u093e\u0930\u093e\u0902\u0936 \u0938\u092e\u091d\u0947\u0902',
  ask: 'ArogyaLens \u0938\u0947 \u092a\u0942\u091b\u0947\u0902',
  recent: '\u0939\u093e\u0932 \u0915\u0947 \u0938\u0924\u094d\u0930',
  uploadTitle:
    '\u0905\u092a\u0928\u093e \u0926\u0938\u094d\u0924\u093e\u0935\u0947\u091c\u093c \u0905\u092a\u0932\u094b\u0921 \u0915\u0930\u0947\u0902',
  chooseFile: '\u092b\u093c\u093e\u0907\u0932 \u091a\u0941\u0928\u0947\u0902',
  analyze:
    '\u0926\u0938\u094d\u0924\u093e\u0935\u0947\u091c\u093c \u0935\u093f\u0936\u094d\u0932\u0947\u0937\u0923 \u0915\u0930\u0947\u0902',
  needFile:
    '\u0915\u0943\u092a\u092f\u093e \u0935\u093f\u0936\u094d\u0932\u0947\u0937\u0923 \u0915\u0947 \u0932\u093f\u090f \u090f\u0915 \u092b\u093c\u093e\u0907\u0932 \u091a\u0941\u0928\u0947\u0902\u0964',
  yourResults: '\u0906\u092a\u0915\u0947 \u092a\u0930\u093f\u0923\u093e\u092e',
  sources: '\u0938\u094d\u0930\u094b\u0924',
  multilingual:
    '\u0906\u092a\u0915\u0940 \u092d\u093e\u0937\u093e \u092e\u0947\u0902 \u0935\u094d\u092f\u093e\u0916\u094d\u092f\u093e',
  listening: '\u0938\u0941\u0928\u0947\u0902',
  explain12:
    '\u091c\u0948\u0938\u0947 \u092e\u0948\u0902 12 \u0938\u093e\u0932 \u0915\u093e \u0939\u0942\u0901',
  backHome: '\u0939\u094b\u092e',
  accessibility: '\u0938\u0941\u0917\u092e\u094d\u092f\u0924\u093e',
}

const kn: Dict = {
  ...en,
  askPlaceholder: 'ಯಾವುದೇ ಆರೋಗ್ಯ ಪ್ರಶ್ನೆ ಕೇಳಿ…',
  askButton: 'ಕೇಳಿ',
  doctors: 'ವೈದ್ಯರನ್ನು ಹುಡುಕಿ',
  emergencyTitle: 'ತುರ್ತು ಪರಿಸ್ಥಿತಿ? ಈಗಲೇ 108 (ಆಂಬ್ಯುಲೆನ್ಸ್) ಅಥವಾ 112 ಗೆ ಕರೆ ಮಾಡಿ.',
  hero: '\u0c86\u0cb0\u0ccb\u0c97\u0ccd\u0caf \u0cae\u0cbe\u0cb9\u0cbf\u0ca4\u0cbf, \u0c8e\u0cb2\u0ccd\u0cb2\u0cb0\u0cbf\u0c97\u0cc2 \u0c85\u0cb0\u0ccd\u0ca5\u0cb5\u0cbe\u0c97\u0cc1\u0cb5\u0c82\u0ca4\u0cc6.',
  scanReport:
    '\u0cb0\u0cbf\u0caa\u0ccb\u0cb0\u0ccd\u0c9f\u0ccd \u0cb8\u0ccd\u0c95\u0ccd\u0caf\u0cbe\u0ca8\u0ccd \u0cae\u0cbe\u0ca1\u0cbf',
  analyze:
    '\u0ca6\u0cbe\u0c96\u0cb2\u0cc6 \u0cb5\u0cbf\u0cb6\u0ccd\u0cb2\u0cc7\u0cb7\u0cbf\u0cb8\u0cbf',
  yourResults:
    '\u0ca8\u0cbf\u0cae\u0ccd\u0cae \u0cab\u0cb2\u0cbf\u0ca4\u0cbe\u0c82\u0cb6\u0c97\u0cb3\u0cc1',
  uploadTitle:
    '\u0ca8\u0cbf\u0cae\u0ccd\u0cae \u0ca6\u0cbe\u0c96\u0cb2\u0cc6\u0caf\u0ca8\u0ccd\u0ca8\u0cc1 \u0c85\u0caa\u0ccd\u200c\u0cb2\u0ccb\u0ca1\u0ccd \u0cae\u0cbe\u0ca1\u0cbf',
  chooseFile: '\u0cab\u0cc8\u0cb2\u0ccd \u0c86\u0caf\u0ccd\u0c95\u0cc6\u0cae\u0cbe\u0ca1\u0cbf',
  needFile:
    '\u0ca6\u0caf\u0cb5\u0cbf\u0c9f\u0ccd\u0c9f\u0cc1 \u0cb5\u0cbf\u0cb6\u0ccd\u0cb2\u0cc7\u0cb7\u0cbf\u0cb8\u0cb2\u0cc1 \u0cab\u0cc8\u0cb2\u0ccd \u0c86\u0caf\u0ccd\u0c95\u0cc6\u0cae\u0cbe\u0ca1\u0cbf.',
  listening: '\u0c95\u0cc7\u0cb3\u0cbf',
  multilingual:
    '\u0ca8\u0cbf\u0cae\u0ccd\u0cae \u0cad\u0cbe\u0cb7\u0cc6\u0caf\u0cb2\u0ccd\u0cb2\u0cbf \u0cb5\u0cbf\u0cb5\u0cb0\u0ca3\u0cc6',
  explain12:
    '\u0ca8\u0cbe\u0ca8\u0cc1 12 \u0cb5\u0cb0\u0ccd\u0cb7\u0ca6\u0cb5\u0ca8\u0c82\u0ca4\u0cc6 \u0cb5\u0cbf\u0cb5\u0cb0\u0cbf\u0cb8\u0cbf',
}

const ta: Dict = {
  ...en,
  askPlaceholder: 'எந்த உடல்நலக் கேள்வியையும் கேளுங்கள்…',
  askButton: 'கேளுங்கள்',
  doctors: 'மருத்துவரைத் தேடுங்கள்',
  emergencyTitle: 'அவசரமா? உடனே 108 (ஆம்புலன்ஸ்) அல்லது 112 ஐ அழையுங்கள்.',
  hero: '\u0b9a\u0bc1\u0b95\u0bbe\u0ba4\u0bbe\u0bb0\u0bae\u0bcd, \u0b85\u0ba9\u0bc8\u0bb5\u0bb0\u0bc1\u0b95\u0bcd\u0b95\u0bc1\u0bae\u0bcd \u0baa\u0bc1\u0bb0\u0bbf\u0baf\u0bc1\u0bae\u0bcd \u0bb5\u0b95\u0bc8\u0baf\u0bbf\u0bb2\u0bcd.',
  scanReport:
    '\u0b85\u0bb1\u0bbf\u0b95\u0bcd\u0b95\u0bc8\u0baf\u0bc8 \u0bb8\u0bcd\u0b95\u0bc7\u0ba9\u0bcd \u0b9a\u0bc6\u0baf\u0bcd',
  analyze:
    '\u0b86\u0bb5\u0ba3\u0ba4\u0bcd\u0ba4\u0bc8 \u0baa\u0b95\u0bc1\u0baa\u0bcd\u0baa\u0bbe\u0baf\u0bcd\u0bb5\u0bc1 \u0b9a\u0bc6\u0baf\u0bcd',
  yourResults:
    '\u0b89\u0b99\u0bcd\u0b95\u0bb3\u0bcd \u0bae\u0bc1\u0b9f\u0bbf\u0bb5\u0bc1\u0b95\u0bb3\u0bcd',
  uploadTitle:
    '\u0b89\u0b99\u0bcd\u0b95\u0bb3\u0bcd \u0b86\u0bb5\u0ba3\u0ba4\u0bcd\u0ba4\u0bc8 \u0baa\u0ba4\u0bbf\u0bb5\u0bc7\u0bb1\u0bcd\u0bb1\u0bb5\u0bc1\u0bae\u0bcd',
  chooseFile:
    '\u0b95\u0bcb\u0baa\u0bcd\u0baa\u0bc8\u0ba4\u0bcd \u0ba4\u0bc7\u0bb0\u0bcd\u0ba8\u0bcd\u0ba4\u0bc6\u0b9f\u0bc1',
  needFile:
    '\u0baa\u0b95\u0bc1\u0baa\u0bcd\u0baa\u0bbe\u0baf\u0bcd\u0bb5\u0bc1 \u0b9a\u0bc6\u0baf\u0bcd\u0baf \u0b92\u0bb0\u0bc1 \u0b95\u0bcb\u0baa\u0bcd\u0baa\u0bc8\u0ba4\u0bcd \u0ba4\u0bc7\u0bb0\u0bcd\u0ba8\u0bcd\u0ba4\u0bc6\u0b9f\u0bc1\u0b95\u0bcd\u0b95\u0bb5\u0bc1\u0bae\u0bcd.',
  listening: '\u0b95\u0bc7\u0bb3\u0bc1\u0b99\u0bcd\u0b95\u0bb3\u0bcd',
  multilingual:
    '\u0b89\u0b99\u0bcd\u0b95\u0bb3\u0bcd \u0bae\u0bca\u0bb4\u0bbf\u0baf\u0bbf\u0bb2\u0bcd \u0bb5\u0bbf\u0bb3\u0b95\u0bcd\u0b95\u0bae\u0bcd',
  explain12:
    '\u0ba8\u0bbe\u0ba9\u0bcd 12 \u0bb5\u0baf\u0ba4\u0bc1 \u0baa\u0bcb\u0bb2 \u0bb5\u0bbf\u0bb3\u0b95\u0bcd\u0b95\u0bc1',
}

const te: Dict = {
  ...en,
  askPlaceholder: 'ఏదైనా ఆరోగ్య ప్రశ్న అడగండి…',
  askButton: 'అడగండి',
  doctors: 'డాక్టర్‌ను వెతకండి',
  emergencyTitle: 'అత్యవసరమా? వెంటనే 108 (అంబులెన్స్) లేదా 112 కు కాల్ చేయండి.',
  hero: '\u0c06\u0c30\u0c4b\u0c17\u0c4d\u0c2f \u0c38\u0c2e\u0c3e\u0c1a\u0c3e\u0c30\u0c02, \u0c05\u0c02\u0c26\u0c30\u0c3f\u0c15\u0c40 \u0c05\u0c30\u0c4d\u0c25\u0c2e\u0c2f\u0c4d\u0c2f\u0c47\u0c32\u0c3e.',
  scanReport:
    '\u0c30\u0c3f\u0c2a\u0c4b\u0c30\u0c4d\u0c1f\u0c4d \u0c38\u0c4d\u0c15\u0c3e\u0c28\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
  analyze:
    '\u0c2a\u0c24\u0c4d\u0c30\u0c3e\u0c28\u0c4d\u0c28\u0c3f \u0c35\u0c3f\u0c36\u0c4d\u0c32\u0c47\u0c37\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f',
  yourResults: '\u0c2e\u0c40 \u0c2b\u0c32\u0c3f\u0c24\u0c3e\u0c32\u0c41',
  uploadTitle:
    '\u0c2e\u0c40 \u0c2a\u0c24\u0c4d\u0c30\u0c3e\u0c28\u0c4d\u0c28\u0c3f \u0c05\u0c2a\u0c4d\u200c\u0c32\u0c4b\u0c21\u0c4d \u0c1a\u0c47\u0c2f\u0c02\u0c21\u0c3f',
  chooseFile: '\u0c2b\u0c48\u0c32\u0c4d \u0c0e\u0c02\u0c1a\u0c41\u0c15\u0c4b\u0c02\u0c21\u0c3f',
  needFile:
    '\u0c26\u0c2f\u0c1a\u0c47\u0c38\u0c3f \u0c35\u0c3f\u0c36\u0c4d\u0c32\u0c47\u0c37\u0c3f\u0c02\u0c1a\u0c21\u0c3e\u0c28\u0c3f\u0c15\u0c3f \u0c2b\u0c48\u0c32\u0c4d\u200c\u0c28\u0c41 \u0c0e\u0c02\u0c1a\u0c41\u0c15\u0c4b\u0c02\u0c21\u0c3f.',
  listening: '\u0c35\u0c3f\u0c28\u0c02\u0c21\u0c3f',
  multilingual: '\u0c2e\u0c40 \u0c2d\u0c3e\u0c37\u0c32\u0c4b \u0c35\u0c3f\u0c35\u0c30\u0c23',
  explain12:
    '\u0c28\u0c47\u0c28\u0c41 12 \u0c0f\u0c33\u0c4d\u0c32\u0c35\u0c3e\u0c21\u0c3f\u0c32\u0c3e \u0c35\u0c3f\u0c35\u0c30\u0c3f\u0c02\u0c1a\u0c02\u0c21\u0c3f',
}

const mr: Dict = {
  ...en,
  askPlaceholder: 'कोणताही आरोग्य प्रश्न विचारा…',
  askButton: 'विचारा',
  doctors: 'डॉक्टर शोधा',
  emergencyTitle: 'आपत्कालीन स्थिती? आत्ताच 108 (रुग्णवाहिका) किंवा 112 वर कॉल करा.',
  hero: '\u0906\u0930\u094b\u0917\u094d\u092f \u092e\u093e\u0939\u093f\u0924\u0940, \u0938\u0930\u094d\u0935\u093e\u0902\u0928\u093e \u0938\u092e\u091c\u0947\u0932 \u0905\u0936\u0940.',
  scanReport: '\u0905\u0939\u0935\u093e\u0932 \u0938\u094d\u0915\u0945\u0928 \u0915\u0930\u093e',
  analyze:
    '\u0926\u0938\u094d\u0924\u090f\u0935\u091c\u093e\u091a\u0947 \u0935\u093f\u0936\u094d\u0932\u0947\u0937\u0923 \u0915\u0930\u093e',
  yourResults: '\u0924\u0941\u092e\u091a\u0947 \u0928\u093f\u0915\u093e\u0932',
  uploadTitle:
    '\u0924\u0941\u092e\u091a\u093e \u0926\u0938\u094d\u0924\u090f\u0935\u091c \u0905\u092a\u0932\u094b\u0921 \u0915\u0930\u093e',
  chooseFile: '\u092b\u093e\u0907\u0932 \u0928\u093f\u0935\u0921\u093e',
  needFile:
    '\u0915\u0943\u092a\u092f\u093e \u0935\u093f\u0936\u094d\u0932\u0947\u0937\u0923\u093e\u0938\u093e\u0920\u0940 \u092b\u093e\u0907\u0932 \u0928\u093f\u0935\u0921\u093e.',
  listening: '\u0910\u0915\u093e',
  multilingual:
    '\u0924\u0941\u092e\u091a\u094d\u092f\u093e \u092d\u093e\u0937\u0947\u0924 \u0938\u094d\u092a\u0937\u094d\u091f\u0940\u0915\u0930\u0923',
  explain12:
    '\u092e\u0940 \u0967\u0968 \u0935\u0930\u094d\u0937\u093e\u0902\u091a\u093e \u0905\u0938\u0932\u094d\u092f\u093e\u0938\u093e\u0930\u0916\u0947 \u0938\u092e\u091c\u093e\u0935\u0942\u0928 \u0938\u093e\u0902\u0917\u093e',
}

const bn: Dict = {
  ...en,
  askPlaceholder: 'যেকোনো স্বাস্থ্য প্রশ্ন জিজ্ঞাসা করুন…',
  askButton: 'জিজ্ঞাসা করুন',
  doctors: 'ডাক্তার খুঁজুন',
  emergencyTitle: 'জরুরি অবস্থা? এখনই 108 (অ্যাম্বুলেন্স) বা 112 নম্বরে ফোন করুন।',
  hero: '\u09b8\u09cd\u09ac\u09be\u09b8\u09cd\u09a5\u09cd\u09af \u09a4\u09a5\u09cd\u09af, \u09b8\u09ac\u09be\u09b0 \u09ac\u09cb\u099d\u09be\u09b0 \u09ae\u09a4\u09cb\u0964',
  scanReport:
    '\u09b0\u09bf\u09aa\u09cb\u09b0\u09cd\u099f \u09b8\u09cd\u0995\u09cd\u09af\u09be\u09a8 \u0995\u09b0\u09c1\u09a8',
  analyze:
    '\u09a8\u09a5\u09bf \u09ac\u09bf\u09b6\u09cd\u09b2\u09c7\u09b7\u09a3 \u0995\u09b0\u09c1\u09a8',
  yourResults: '\u0986\u09aa\u09a8\u09be\u09b0 \u09ab\u09b2\u09be\u09ab\u09b2',
  uploadTitle:
    '\u0986\u09aa\u09a8\u09be\u09b0 \u09a8\u09a5\u09bf \u0986\u09aa\u09b2\u09cb\u09a1 \u0995\u09b0\u09c1\u09a8',
  chooseFile: '\u09ab\u09be\u0987\u09b2 \u09ac\u09c7\u099b\u09c7 \u09a8\u09bf\u09a8',
  needFile:
    '\u09ac\u09bf\u09b6\u09cd\u09b2\u09c7\u09b7\u09a3\u09c7\u09b0 \u099c\u09a8\u09cd\u09af \u098f\u0995\u099f\u09bf \u09ab\u09be\u0987\u09b2 \u09ac\u09c7\u099b\u09c7 \u09a8\u09bf\u09a8\u0964',
  listening: '\u09b6\u09c1\u09a8\u09c1\u09a8',
  multilingual:
    '\u0986\u09aa\u09a8\u09be\u09b0 \u09ad\u09be\u09b7\u09be\u09af\u09bc \u09ac\u09cd\u09af\u09be\u0996\u09cd\u09af\u09be',
  explain12:
    '\u0986\u09ae\u09bf \u09af\u09c7\u09a8 \u09e7\u09e8 \u09ac\u099b\u09b0\u09c7\u09b0, \u098f\u09ad\u09be\u09ac\u09c7 \u09ac\u09cd\u09af\u09be\u0996\u09cd\u09af\u09be \u0995\u09b0\u09c1\u09a8',
}

const packs: Record<AppLanguage, Dict> = { en, hi, kn, ta, te, mr, bn }

/** Translates a UI string key, falling back to English. */
export function t(language: AppLanguage, key: keyof typeof en): string {
  return packs[language]?.[key] ?? en[key] ?? String(key)
}
