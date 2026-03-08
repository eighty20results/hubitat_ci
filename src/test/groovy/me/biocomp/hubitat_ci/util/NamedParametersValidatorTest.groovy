package me.biocomp.hubitat_ci.util

import me.biocomp.hubitat_ci.validation.Flags
import me.biocomp.hubitat_ci.validation.NamedParametersValidator
import groovy.transform.TypeChecked
import spock.lang.Specification

class NamedParametersValidatorTest extends
        Specification
{
    @TypeChecked
    def makeStringValidator(String name)
    {
        return NamedParametersValidator.make {
            stringParameter(name, notRequired(), canBeEmpty())
        }
    }

    @TypeChecked
    def makeEnumStringValidator(String name, List<String> values)
    {
        return NamedParametersValidator.make {
            enumStringParameter(name, notRequired(), values)
        }
    }

    def "String validator works with both String and GString"()
    {
        setup:
            def answer = 42

        expect:
            makeStringValidator("val1").validate("ctx", [val1: "someTextVal"], EnumSet.noneOf(Flags))
            makeStringValidator("val1").validate("ctx", [val1: "answer is ${answer}"], EnumSet.noneOf(Flags))
    }

    def "String enum validator works with both String and GString"()
    {
        setup:
            def answer = 42

        expect:
            makeEnumStringValidator("val1", ["someTextVal"]).validate("ctx", [val1: "someTextVal"], EnumSet.noneOf(Flags))
            makeEnumStringValidator("val1", ["answer42"]).validate("ctx", [val1: "answer${answer}"], EnumSet.noneOf(Flags))
    }

    def "String enum validator accepts exact case match without warning"()
    {
        setup:
            def validator = makeEnumStringValidator("status", ["open", "closed", "opening", "closing"])

        expect:
            validator.validate("ctx", [status: "open"], EnumSet.noneOf(Flags))
            validator.validate("ctx", [status: "closed"], EnumSet.noneOf(Flags))
    }

    def "String enum validator accepts case-insensitive match (Hubitat compatibility)"()
    {
        setup:
            def validator = makeEnumStringValidator("status", ["open", "closed", "opening", "closing"])
            // Capture stderr to verify warning is logged
            def originalErr = System.err
            def capturedErr = new ByteArrayOutputStream()
            System.err = new PrintStream(capturedErr)

        when:
            validator.validate("ctx", [status: "OPEN"], EnumSet.noneOf(Flags))

        then:
            notThrown(AssertionError)
            capturedErr.toString().contains("WARNING")
            capturedErr.toString().contains("OPEN")
            capturedErr.toString().contains("open")

        cleanup:
            System.err = originalErr
    }

    def "String enum validator rejects completely invalid values"()
    {
        setup:
            def validator = makeEnumStringValidator("status", ["open", "closed"])

        when:
            validator.validate("ctx", [status: "invalid"], EnumSet.noneOf(Flags))

        then:
            AssertionError e = thrown()
            e.message.contains("invalid")
            e.message.contains("not supported")
    }

    def "String enum validator handles mixed case variations"()
    {
        setup:
            def validator = makeEnumStringValidator("mode", ["Morning", "Evening", "Night"])
            def originalErr = System.err
            def capturedErr = new ByteArrayOutputStream()
            System.err = new PrintStream(capturedErr)

        when:
            validator.validate("ctx", [mode: "morning"], EnumSet.noneOf(Flags))

        then:
            notThrown(AssertionError)
            capturedErr.toString().contains("WARNING")
            capturedErr.toString().contains("morning")
            capturedErr.toString().contains("Morning")

        cleanup:
            System.err = originalErr
    }
}
