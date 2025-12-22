package me.biocomp.hubitat_ci.device.metadata

import groovy.transform.TypeChecked
import me.biocomp.hubitat_ci.validation.BooleanInputValueFactory
import me.biocomp.hubitat_ci.validation.DefaultAndUserValues
import me.biocomp.hubitat_ci.validation.Flags
import me.biocomp.hubitat_ci.validation.IInputValueFactory
import me.biocomp.hubitat_ci.validation.InputCommon
import me.biocomp.hubitat_ci.validation.NamedParametersValidator
import me.biocomp.hubitat_ci.validation.NumberInputValueFactory
import me.biocomp.hubitat_ci.validation.TextInputValueFactory

/**
 * Information about 'input' in Device.
 */
@TypeChecked
class DeviceInput extends InputCommon {
    private static final HashMap<String, IInputValueFactory> validStaticInputTypes =
            [bool    : new BooleanInputValueFactory(),
             decimal : new NumberInputValueFactory(),
             email   : new TextInputValueFactory(),
             enum    : new TextInputValueFactory(),
             number  : new NumberInputValueFactory(),
             password: new TextInputValueFactory(),
             phone   : new NumberInputValueFactory(),
             time    : new TextInputValueFactory(),
             text    : new TextInputValueFactory(),
             paragraph: new TextInputValueFactory(),
             date    : new TextInputValueFactory(),
             textarea: new TextInputValueFactory(),
             mode    : new TextInputValueFactory(),
             hub     : new TextInputValueFactory()
            ] as HashMap<String, IInputValueFactory>
    // capability.* and device.* selectors treated structurally

    private static final NamedParametersValidator inputOptionsValidator = NamedParametersValidator.make {
        stringParameter("name", required(), canBeEmpty(), [Flags.DontValidateDeviceInputName])
        enumStringParameter("type", required(), [
                'bool',
                'decimal',
                'email',
                'enum',
                'number',
                'password',
                'phone',
                'time',
                'text',
                'paragraph',
                'date',
                'textarea',
                'mode',
                'hub'
        ])
        stringParameter("title", notRequired(), canBeEmpty())
        stringParameter("description", notRequired(), canBeEmpty())
        objParameter("defaultValue", notRequired(), canBeNull())
        boolParameter("required", notRequired())
        boolParameter("displayDuringSetup", notRequired())
        boolParameter("multiple", notRequired())
        boolParameter("submitOnChange", notRequired())
        numericRangeParameter("range", notRequired())
        listOfStringsParameter("options", notRequired())
    }

    DeviceInput(Map unnamedOptions, Map options, EnumSet<Flags> validationFlags) {
        super(unnamedOptions, options, validationFlags, validStaticInputTypes)
    }

    @Override
    boolean validateInputBasics()
    {
        if (!validationFlags.contains(Flags.DontValidatePreferences)) {
            def typeValue = (options?.type ?: unnamedOptions?.type)
            if (typeValue instanceof String && (typeValue.toLowerCase().startsWith('capability.') || typeValue.toLowerCase().startsWith('device.'))) {
                return true
            }

            inputOptionsValidator.validate(toString(),
                    unnamedOptions,
                    options,
                    validationFlags,
                    validationFlags.contains(Flags.AllowMissingDeviceInputNameOrType) ? EnumSet.of(
                            NamedParametersValidator.ValidatorOption.IgnoreMissingMandatoryInputs) : EnumSet.noneOf(
                            NamedParametersValidator.ValidatorOption))

            // enum requires options only in strict mode
            if ((options?.type == 'enum' || unnamedOptions?.type == 'enum') && validationFlags.contains(Flags.StrictEnumInputValidation)) {
                def opts = options?.options
                assert (opts instanceof Collection && opts.size() > 0): "Input ${this} of type enum must provide options"
            }

            return true
        }

        return false
    }

    @Override
    IInputValueFactory typeNotFoundInTypeTable(String inputType) {
        if (inputType?.toLowerCase()?.startsWith('capability.') || inputType?.toLowerCase()?.startsWith('device.')) {
            // Treat capability./device. selectors as text inputs; structural validation happens elsewhere.
            return new TextInputValueFactory()
        }
        assert false: "Input ${this}'s type ${inputType} is not supported. Valid types are: ${validStaticInputTypes.keySet()} or capability./device. selectors"
    }
}
