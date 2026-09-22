package com.tz.forensics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Configuration
public class CountryConfig implements WebMvcConfigurer {

    public static final Map<String, CountryInfo> COUNTRIES = new LinkedHashMap<>();

    static {
        COUNTRIES.put("TZ", new CountryInfo("TZ", "Tanzania", "+255",
            "Police: 112", "Regulator: +255 22 219 6000",
            List.of("Dar es Salaam","Arusha","Mwanza","Dodoma","Mbeya","Morogoro","Tanga","Zanzibar","Kilimanjaro","Tabora","Kigoma","Iringa","Mtwara","Ruvuma","Singida","Shinyanga","Kagera","Mara","Manyara","Rukwa","Katavi","Njombe","Simiyu","Geita","Songwe","Pwani","Lindi")));

        COUNTRIES.put("KE", new CountryInfo("KE", "Kenya", "+254",
            "Police: 999", "Regulator: +254 20 4242000",
            List.of("Nairobi","Mombasa","Kisumu","Nakuru","Eldoret","Thika","Malindi","Kitale","Garissa","Nyeri","Machakos","Meru","Lamu","Kericho","Kakamega")));

        COUNTRIES.put("UG", new CountryInfo("UG", "Uganda", "+256",
            "Police: 999", "Regulator: +256 41 710 0000",
            List.of("Kampala","Gulu","Lira","Mbarara","Jinja","Mbale","Masaka","Entebbe","Arua","Fort Portal","Kabale","Soroti","Hoima","Mukono","Kasese")));

        COUNTRIES.put("RW", new CountryInfo("RW", "Rwanda", "+250",
            "Police: 112", "Regulator: +250 788 195 000",
            List.of("Kigali","Butare","Gitarama","Ruhengeri","Gisenyi","Byumba","Cyangugu","Kibuye","Kibungo","Rwamagana","Nyanza","Nyagatare","Rusizi")));

        COUNTRIES.put("BI", new CountryInfo("BI", "Burundi", "+257",
            "Police: 117", "Regulator: +257 22 27 8888",
            List.of("Bujumbura","Gitega","Ngozi","Rumonge","Muyinga","Ruyigi","Kayanza","Makamba","Bururi","Cibitoke")));

        COUNTRIES.put("SS", new CountryInfo("SS", "South Sudan", "+211",
            "Police: 999", "Regulator: +211 920 000000",
            List.of("Juba","Wau","Malakal","Yei","Aweil","Rumbek","Bor","Torit","Bentiu","Yambio")));

        COUNTRIES.put("CD", new CountryInfo("CD", "DR Congo", "+243",
            "Police: 112", "Regulator: +243 81 555 5555",
            List.of("Kinshasa","Lubumbashi","Mbuji-Mayi","Kisangani","Kananga","Likasi","Bukavu","Goma","Kolwezi","Kikwit")));

        COUNTRIES.put("ZM", new CountryInfo("ZM", "Zambia", "+260",
            "Police: 991", "Regulator: +260 211 246 800",
            List.of("Lusaka","Kitwe","Ndola","Kabwe","Chingola","Mufulira","Livingstone","Luanshya","Chipata","Kasama")));

        COUNTRIES.put("MW", new CountryInfo("MW", "Malawi", "+265",
            "Police: 997", "Regulator: +265 1 774 622",
            List.of("Lilongwe","Blantyre","Mzuzu","Zomba","Kasungu","Mangochi","Karonga","Salima","Nkhotakota","Dedza")));

        COUNTRIES.put("MZ", new CountryInfo("MZ", "Mozambique", "+258",
            "Police: 119", "Regulator: +258 21 303 030",
            List.of("Maputo","Matola","Beira","Nampula","Chimoio","Nacala","Quelimane","Tete","Pemba","Xai-Xai")));

        COUNTRIES.put("ZA", new CountryInfo("ZA", "South Africa", "+27",
            "Police: 10111", "Regulator: +27 11 566 3000",
            List.of("Johannesburg","Cape Town","Durban","Pretoria","Port Elizabeth","Bloemfontein","East London","Polokwane","Nelspruit","Kimberley")));

        COUNTRIES.put("NG", new CountryInfo("NG", "Nigeria", "+234",
            "Police: 112", "Regulator: +234 9 461 7000",
            List.of("Lagos","Abuja","Kano","Ibadan","Port Harcourt","Benin City","Kaduna","Enugu","Aba","Onitsha")));

        COUNTRIES.put("GH", new CountryInfo("GH", "Ghana", "+233",
            "Police: 191", "Regulator: +233 30 270 1220",
            List.of("Accra","Kumasi","Tamale","Sekondi-Takoradi","Cape Coast","Sunyani","Ho","Koforidua","Wa","Bolgatanga")));

        COUNTRIES.put("IN", new CountryInfo("IN", "India", "+91",
            "Police: 100", "Regulator: +91 11 2342 3000",
            List.of("Mumbai","Delhi","Bangalore","Chennai","Kolkata","Hyderabad","Pune","Ahmedabad","Jaipur","Surat")));

        COUNTRIES.put("GB", new CountryInfo("GB", "United Kingdom", "+44",
            "Police: 999", "Regulator: +44 20 7981 3000",
            List.of("London","Manchester","Birmingham","Glasgow","Liverpool","Edinburgh","Bristol","Cardiff","Belfast","Leeds")));

        COUNTRIES.put("US", new CountryInfo("US", "United States", "+1",
            "Police: 911", "Regulator: +1 202 418 1000",
            List.of("New York","Los Angeles","Chicago","Houston","Phoenix","Philadelphia","San Antonio","San Diego","Dallas","San Jose")));

        COUNTRIES.put("OTHER", new CountryInfo("OTHER", "Other Country", "",
            "Local emergency: 112", "Local regulator",
            List.of("Other")));
    }

    public static CountryInfo getCountry(String code) {
        return COUNTRIES.getOrDefault(code != null ? code : "TZ", COUNTRIES.get("TZ"));
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(new Locale("en"));
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    public static class CountryInfo {
        public final String code, name, dialCode, policeContact, regulatorContact;
        public final List<String> regions;

        public CountryInfo(String code, String name, String dialCode,
                          String policeContact, String regulatorContact, List<String> regions) {
            this.code = code;
            this.name = name;
            this.dialCode = dialCode;
            this.policeContact = policeContact;
            this.regulatorContact = regulatorContact;
            this.regions = regions;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getDialCode() { return dialCode; }
        public String getPoliceContact() { return policeContact; }
        public String getRegulatorContact() { return regulatorContact; }
        public List<String> getRegions() { return regions; }
    }
}
