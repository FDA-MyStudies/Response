package org.labkey.response.data;

import com.google.common.base.Enums;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum Language
{
    // Language codes and names sourced from https://developer.box.com/guides/api-calls/language-codes/
    bn("Bengali"),
    da("Danish"),
    de("German"),
    en("English (US)"),
    gb("English (UK)"),
    e2("English (Canada)"),
    e3("English (Australia)"),
    s2("Spanish (Latin America)"),
    es("Spanish")
        {
            @Override
            public String getTokenIsRequiredErrorMessage()
            {
                return "Se requiere un token";
            }
        },
    fi("Finnish"),
    fr("French"),
    f2("French (Canada)"),
    hi("Hindi"),
    it("Italian"),
    ja("Japanese"),
    ko("Korean"),
    nb("Norwegian (Bokmal)"),
    nl("Dutch"),
    pl("Polish"),
    pt("Portuguese"),
    ru("Russian"),
    sv("Swedish"),
    tr("Turkish"),
    zh("Chinese (Simplified)"),
    zt("Chinese (Traditional)");

    private final String _friendlyName;

    Language(String friendlyName)
    {
        _friendlyName = friendlyName;
    }

    public String getFriendlyName()
    {
        return _friendlyName;
    }

    // Default to English; override to provide translation
    public String getTokenIsRequiredErrorMessage()
    {
        return "Token is required";
    }

    // Return a Language enum constant given a two-character code. Null or unknown code default to US English (en).
    public static @NotNull Language getLanguage(@Nullable String code)
    {
        return null != code ? Enums.getIfPresent(Language.class, code).or(Language.en) : Language.en;
    }
}
