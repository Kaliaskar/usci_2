function isEmpty(value) {
    return value === undefined || value === null || value.length === 0;
}

function getProfiles() {
    if (isSenchaMode)
        return ['profile://FSystem', 'profile://eToken'];

    var profilesString = document.app.getProfileNames('|');
    return profilesString.split('|');
}

function getCertificates(profile, password) {
    if (isSenchaMode)
        return ['C=KZ,O=Веб портал НБ РК,CN=Попова Светлана,UID=IIN851127400204',
            'CN=ИБРАИМОВ БАУЫРЖАН,SURNAME=ИБРАИМОВ,SERIALNUMBER=IIN880318301425,C=KZ,L=АЛМАТЫ,ST=АЛМАТЫ,GIVENNAME=КАДЫРБЕКОВИЧ,E=KZ.BAURZHAN@GMAIL.COM'];

    var certificatesInfo = document.app.getCertificatesInfo(profile, password, 0, '', true, false, '|');
    return certificatesInfo.split('|');
}

function getUserNameByCertificate(certificate) {
    if (!certificate)
        throw new Error('Сертификато не может быть = NULL');

    var index = certificate.indexOf('CN=');
    if (index < 0)
        throw new Error('Ошибка получение имени пользователя из сертификата');

    index += 3;

    var userName = '';
    for (var i = index; i < certificate.length; i++) {
        var ch = certificate.charAt(i);
        if (ch === ',')
            break;

        userName += ch;
    }

    return userName;
}

function signHash(value, certificate, profile, password) {
    if (isSenchaMode)
        return 'TEST SIGNATURE';

    return document.app.createPKCS7(value, 0, null, certificate, true, profile, password, '1.3.6.1.4.1.6801.1.5.8', true);
}