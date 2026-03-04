package me.biocomp.hubitat_ci.validation

import groovy.json.JsonSlurperClassic
import groovy.sql.Sql
import groovy.transform.CompileStatic
import groovy.transform.Field
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovy.xml.MarkupBuilder
import groovy.xml.StreamingMarkupBuilder
import me.biocomp.hubitat_ci.api.common_api.AsyncResponse
import me.biocomp.hubitat_ci.api.common_api.ColorUtils
import me.biocomp.hubitat_ci.api.common_api.ParentDeviceWrapper
import me.biocomp.hubitat_ci.api.device_api.zigbee.DataType
import me.biocomp.hubitat_ci.api.http.ContentType
import me.biocomp.hubitat_ci.api.http.HttpResponseException
import me.biocomp.hubitat_ci.api.http.Method
import me.biocomp.hubitat_ci.util.AddValidationAfterEachMethodCompilationCustomizer
import me.biocomp.hubitat_ci.util.DoNotCallMeBinding
import me.biocomp.hubitat_ci.util.LoggingCompilationCustomizer
import me.biocomp.hubitat_ci.util.RemovePrivateFromScriptCompilationCustomizer
import me.biocomp.hubitat_ci.util.SandboxClassLoader
import me.biocomp.hubitat_ci.util.integration.TimeKeeper
import groovy.json.JsonBuilder
import groovy.time.TimeCategory
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.CompilationCustomizer
import org.codehaus.groovy.control.customizers.SecureASTCustomizer
import org.codehaus.groovy.control.customizers.SourceAwareCustomizer
import sun.util.calendar.ZoneInfo

import java.math.RoundingMode
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAdjusters
import java.util.BitSet
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Semaphore
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.DeflaterInputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream
import java.util.zip.InflaterOutputStream
import java.util.zip.ZipError
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
// Note: org.apache.commons.lang3.* and org.quartz.CronExpression are whitelisted by
// class name string only (not imported directly) because they are third-party runtime
// dependencies that may not be on the compile classpath of hubitat_ci itself.

@TypeChecked
class ValidatorBase {
    private final static Set<String> originalForbiddenExpressions =
            ["execute",
             "getClass",
             "getMetaClass",
             "setMetaClass",
             "propertyMissing",
             "methodMissing",
             "invokeMethod",
             "print",
             "println",
             "printf",
             "sleep",
             "getProducedPreferences", // Script's test-only use method
             "producedPreferences", // Script's test-only use property
             "getProducedDefinition", // Script's test-only use method
             "producedDefinition" // Script's test-only use property
            ] as HashSet

    private final static Set<String> forumDocsAllowedImports = [] as HashSet

    private static HashSet<Class> classOriginalWhiteList = [java.lang.Object,
                                                            java.lang.Exception,
                                                            java.lang.Throwable,
                                                            groovy.lang.GString,
                                                            org.codehaus.groovy.runtime.InvokerHelper,
                                                            ArrayList,
                                                            int,
                                                            boolean,
                                                            byte,
                                                            char,
                                                            short,
                                                            long,
                                                            float,
                                                            double,
                                                            BigDecimal,
                                                            BigInteger,
                                                            RoundingMode,
                                                            Boolean,
                                                            Byte,
                                                            ByteArrayInputStream,
                                                            ByteArrayOutputStream,
                                                            Calendar,
                                                            Closure,
                                                            Collection,
                                                            Collections,
                                                            Range,
                                                            IntRange,
                                                            ObjectRange,
                                                            Date,
                                                            TimeKeeper,
                                                            DecimalFormat,
                                                            Double,
                                                            Float,
                                                            GregorianCalendar,
                                                            HashMap,
                                                            //HashMap.Entry,
                                                            //                    HashMap,
                                                            //                    HashMap.KeySet,
                                                            //                    HashMap.Values,
                                                            HashSet,
                                                            Integer,
                                                            JsonBuilder,
                                                            LinkedHashMap,
                                                            //LinkedHashMap.Entry,
                                                            LinkedHashSet,
                                                            LinkedList,
                                                            List,
                                                            Long,
                                                            Map,
                                                            MarkupBuilder,
                                                            Math,
                                                            Random,
                                                            Set,
                                                            Short,
                                                            SimpleDateFormat,
                                                            String,
                                                            StringBuilder,
                                                            StringBuffer,
                                                            OutputStream,
                                                            StringReader,
                                                            StringWriter,
                                                            CharSequence,
                                                            Number,
                                                            Character,
                                                            BitSet,
                                                            Locale,
                                                            Matcher,
                                                            //SubList,
                                                            TimeCategory,
                                                            TimeZone,
                                                            TreeMap,
                                                            //                    TreeMap.Entry,
                                                            //                    TreeMap.KeySet,
                                                            //                    TreeMap.Values,
                                                            TreeSet,
                                                            URLDecoder,
                                                            URLEncoder,
                                                            UUID,
                                                            ZoneInfo,
                                                            //com.amazonaws.services.s3.model.S3Object,
                                                            //com.amazonaws.services.s3.model.S3ObjectInputStream,
                                                            com.sun.org.apache.xerces.internal.dom.DocumentImpl,
                                                            com.sun.org.apache.xerces.internal.dom.ElementImpl,
                                                            groovy.json.JsonOutput,
                                                            groovy.json.JsonSlurper,
                                                            groovy.util.Node,
                                                            groovy.util.NodeList,
                                                            groovy.util.XmlParser,
                                                            groovy.util.XmlSlurper,
                                                            groovy.xml.XmlUtil,
                                                            java.net.URI,
                                                            // java.util.RandomAccessSubList,
                                                            //org.apache.commons.codec.binary.Base64,
                                                            //org.apache.xerces.dom.DocumentImpl,
                                                            //org.apache.xerces.dom.ElementImpl,
                                                            org.codehaus.groovy.runtime.EncodingGroovyMethods,
                                                            //org.json.JSONArray,
                                                            //org.json.JSONException,
                                                            //org.json.JSONObject,
                                                            //org.json.JSONObject.Null,
                                                            me.biocomp.hubitat_ci.api.Protocol,
                                                            me.biocomp.hubitat_ci.api.common_api.HubAction,
                                                            me.biocomp.hubitat_ci.api.common_api.HubResponse,
                                                            me.biocomp.hubitat_ci.api.common_api.Location

    ] as HashSet<Class>

    private static HashSet<Class> forumDocsWhiteList = classOriginalWhiteList + ([
            // Groovy extensions
            Field,
            CompileStatic,
            JsonSlurperClassic,
            StreamingMarkupBuilder,
            Sql,

            // Security / crypto
            MessageDigest,
            SecureRandom,
            InvalidKeyException,
            Signature,
            PrivateKey,
            KeyFactory,
            PKCS8EncodedKeySpec,
            Mac,
            SecretKeySpec,
            IvParameterSpec,
            Cipher,

            // Concurrency
            ConcurrentHashMap,
            ConcurrentLinkedQueue,
            CopyOnWriteArrayList,
            Semaphore,
            SynchronousQueue,
            TimeUnit,
            AtomicInteger,
            AtomicIntegerArray,

            // Regex
            Pattern,
            Matcher,

            // Networking
            URL,

            // java.time.* - full set permitted by Hubitat
            Clock,
            Instant,
            Duration,
            Period,
            LocalDate,
            LocalDateTime,
            LocalTime,
            MonthDay,
            OffsetDateTime,
            OffsetTime,
            Year,
            YearMonth,
            ZonedDateTime,
            ZoneId,
            ZoneOffset,
            DayOfWeek,
            Month,
            DateTimeFormatter,
            DateTimeFormatterBuilder,
            TemporalAdjusters,

            // java.util.zip.*
            Deflater,
            Inflater,
            DeflaterInputStream,
            DeflaterOutputStream,
            GZIPInputStream,
            GZIPOutputStream,
            InflaterInputStream,
            InflaterOutputStream,
            ZipInputStream,
            ZipOutputStream,
            DataFormatException,
            ZipException,
            ZipError,

            // Note: org.apache.commons.lang3.*, org.quartz.CronExpression,
            // com.google.common.util.concurrent.Striped, com.nimbusds.jose.*,
            // com.nimbusds.jwt.*, javax.jmdns.*, su.litvak.chromecast.*, org.json.*
            // and org.apache.commons.codec.binary.Base64 are whitelisted by class-name
            // string only (see addAll in ValidatorBase constructor) because those jars
            // are not hubitat_ci compile-time dependencies.
            // Pre-classloader-mapping names (hubitat.*, com.hubitat.*, groovyx.net.http.*)
            // are also added as strings only because they differ from the mapped class names.

            // Hubitat helper classes mapped via SandboxClassLoader
            ColorUtils,
            me.biocomp.hubitat_ci.api.common_api.HexUtils,
            me.biocomp.hubitat_ci.api.common_api.RMUtils,
            me.biocomp.hubitat_ci.api.common_api.ZigbeeUtils,
            AsyncResponse,
            ParentDeviceWrapper,

            // Zigbee DataType
            DataType,

            // groovyx.net.http
            HttpResponseException,
            Method,
            ContentType,
    ] as Collection<Class>) as HashSet<Class>

    private final HashSet<String> classNameWhiteList
    private final HashSet<String> forbiddenExpressions

    static private HashSet<String> initClassNames(List<Class> extraAllowedClasses, HashSet<Class> baseWhitelist) {
        return (baseWhitelist + (extraAllowedClasses as HashSet)).collect { it.name } as HashSet;
    }

    static private HashSet<String> initForbiddenExpressions(List<String> extraAllowedExpressions) {
        return (originalForbiddenExpressions - (extraAllowedExpressions as HashSet)) as HashSet;
    }

    ValidatorBase(
            EnumSet<Flags> setOfFlags,
            List<Class> extraAllowedClasses,
            List<String> extraAllowedExpressions)
    {
        this.flags = setOfFlags
        def baseWhitelist = flags.contains(Flags.AllowLegacyImports) ? classOriginalWhiteList : forumDocsWhiteList
        this.classNameWhiteList = initClassNames(extraAllowedClasses, baseWhitelist)
        if (!flags.contains(Flags.AllowLegacyImports)) {
            // Append string-only entries that cannot be derived from forumDocsWhiteList.collect { it.name }:
            //  (a) third-party jars not on the hubitat_ci compile classpath, and
            //  (b) pre-classloader-mapping names that scripts use before SandboxClassLoader remaps them,
            //  (c) inner-class name variants not representable as Class literals.
            // All other allowed imports are already covered by initClassNames(forumDocsWhiteList).
            this.classNameWhiteList.addAll([
                    // Inner-class variant not representable as a Class literal
                    'java.util.Locale$Builder',
                    'java.util.Locale.Builder',

                    // Apache Commons (not on compile classpath)
                    'org.apache.commons.codec.binary.Base64',
                    'org.apache.commons.lang3.StringEscapeUtils',
                    'org.apache.commons.lang3.StringUtils',
                    'org.apache.commons.lang3.math.NumberUtils',
                    'org.apache.commons.lang3.time.DateUtils',
                    'org.apache.xerces.dom.DocumentImpl',
                    'org.apache.xerces.dom.ElementImpl',

                    // org.json (not on compile classpath)
                    'org.json.JSONArray',
                    'org.json.JSONException',
                    'org.json.JSONObject',
                    'org.json.JSONObject$Null',
                    'org.json.JSONObject.Null',

                    // Quartz (not on compile classpath)
                    'org.quartz.CronExpression',

                    // Google Guava (not on compile classpath)
                    'com.google.common.util.concurrent.Striped',

                    // Nimbus JOSE + JWT (not on compile classpath)
                    'com.nimbusds.jose.PlainObject',
                    'com.nimbusds.jose.JWSObject',
                    'com.nimbusds.jose.Payload',
                    'com.nimbusds.jose.JWEObject',
                    'com.nimbusds.jose.JWEHeader',
                    'com.nimbusds.jose.JOSEObjectType',
                    'com.nimbusds.jose.JWSAlgorithm',
                    'com.nimbusds.jose.JWSSigner',
                    'com.nimbusds.jose.JWTHeader',
                    'com.nimbusds.jose.JWTHeader$Builder',
                    'com.nimbusds.jose.JWTClaimsSet',
                    'com.nimbusds.jose.JWTClaimsSet$Builder',
                    'com.nimbusds.jose.jwk.RSAKey',
                    'com.nimbusds.jose.crypto.RSASSASigner',
                    'com.nimbusds.jose.JWSHeader',
                    'com.nimbusds.jose.JWSHeader$Builder',
                    'com.nimbusds.jose.jwk.JWK',
                    'com.nimbusds.jose.util.X509CertUtils',
                    'com.nimbusds.jwt.PlainJWT',
                    'com.nimbusds.jwt.SignedJWT',
                    'com.nimbusds.jwt.EncryptedJWT',
                    'com.nimbusds.jwt.JWTClaimsSet',
                    'com.nimbusds.jwt.JWTClaimsSet$Builder',

                    // jmDNS (not on compile classpath)
                    'javax.jmdns.JmDNS',
                    'javax.jmdns.ServiceEvent',
                    'javax.jmdns.impl.ServiceEventImpl',

                    // ChromeCast (not on compile classpath)
                    'su.litvak.chromecast.api.v2.ChromeCasts',

                    // Hubitat helper interfaces (pre-classloader-mapping names)
                    'hubitat.helper.interfaces.EventStream',
                    'hubitat.helper.InterfaceHelper',
                    'hubitat.helper.interfaces.Mqtt',
                    'hubitat.helper.interfaces.RawSocket',
                    'hubitat.helper.interfaces.WebSocket',
                    'hubitat.helper.ColorUtils',
                    'hubitat.helper.HexUtils',
                    'hubitat.helper.RMUtils',
                    'hubitat.helper.ZigbeeUtils',

                    // Hubitat scheduling (pre-classloader-mapping name)
                    'hubitat.scheduling.AsyncResponse',

                    // Hubitat app/device wrappers (pre-classloader-mapping names)
                    'com.hubitat.app.ParentDeviceWrapper',
                    'hubitat.app.ParentDeviceWrapper',

                    // Zigbee DataType (pre-classloader-mapping names)
                    'com.hubitat.zigbee.DataType',
                    'hubitat.zigbee.DataType',

                    // groovyx.net.http (user-visible names; forumDocsWhiteList holds the me.biocomp.hubitat_ci.api.http.* wrappers)
                    'groovyx.net.http.HttpResponseException',
                    'groovyx.net.http.Method',
                    'groovyx.net.http.ContentType',
            ])
        }
        this.forbiddenExpressions = initForbiddenExpressions(extraAllowedExpressions)
    }

    ValidatorBase(
            List<Flags> listOfFlags, List<Class> extraAllowedClasses, List<String> extraAllowedExpressions)
    {
        this(flagsListToSet(listOfFlags), extraAllowedClasses, extraAllowedExpressions)
    }

    static private EnumSet<Flags> flagsListToSet(List<Flags> listOfFlags)
    {
        def setOfFlags = EnumSet.noneOf(Flags)
        listOfFlags?.each { setOfFlags.add(it) }
        return setOfFlags
    }

    boolean hasFlag(Flags flag) {
        return flags.contains(flag)
    }

    EnumSet<Flags> getFlags()
    {
        return flags
    }

    private final EnumSet<Flags> flags

    private void restrictScript(CompilerConfiguration options) {
        if(hasFlag(Flags.DontRestrictGroovy)) {
            return
        }

        def scz = new SecureASTCustomizer()

        def privateForbiddenExpressions = this.forbiddenExpressions
        def privateClassNameWhiteList = this.classNameWhiteList

        def checkerCapture = { expr ->
            if (expr instanceof MethodCallExpression) {
                if (expr.getObjectExpression() instanceof RangeExpression) {
                    return !privateForbiddenExpressions.contains(expr.methodAsString)
                }
                return !privateForbiddenExpressions.contains(expr.methodAsString) && isClassAllowed(privateClassNameWhiteList,
                        expr.getObjectExpression().getType())
            }

            if (expr instanceof PropertyExpression) {
                return !privateForbiddenExpressions.contains(expr.propertyAsString) && isClassAllowed(privateClassNameWhiteList,
                        expr.getObjectExpression().getType())
            }

            if (expr instanceof AttributeExpression) {
                return !privateForbiddenExpressions.contains(expr.propertyAsString) && isClassAllowed(privateClassNameWhiteList,
                        expr.getObjectExpression().getType())
            }

            if (expr instanceof VariableExpression) {
                return !privateForbiddenExpressions.contains(expr.name)
            }

            if (expr instanceof StaticMethodCallExpression) {
                return !privateForbiddenExpressions.contains(expr.methodAsString) && isClassAllowed(privateClassNameWhiteList, expr.getOwnerType())
            }

            return true;
        }

        checkerCapture.resolveStrategy = Closure.DELEGATE_ONLY
        checkerCapture.delegate = this as ValidatorBase

        def checker = checkerCapture as SecureASTCustomizer.ExpressionChecker

        scz.addExpressionCheckers(checker)

        def sac = new SourceAwareCustomizer(scz)

        sac.sourceUnitValidator = {
            return true
        }

        sac.classValidator = { ClassNode cn ->
            if (!cn.scriptBody) {
                throw new SecurityException("Can't define classes in the script, but you defined '${cn}'")
            }

            return true
        }

        options.addCompilationCustomizers(sac)
    }

    protected static boolean isClassAllowed(HashSet<String> classNameWhiteList, ClassNode classNode) {
        if (classNameWhiteList.contains(classNode.name)) {
            return true
        }

        if (classNode.name.startsWith('me.biocomp.hubitat_ci.api.device_api.zwave') ||
            classNode.name.startsWith('me.biocomp.hubitat_ci.api.device_api.zigbee'))
        {
            return true
        }

        return classNode.isScript()
    }

    private static void makePrivatePublic(CompilerConfiguration options) {
        options.addCompilationCustomizers(new RemovePrivateFromScriptCompilationCustomizer())
    }

    private static void validateAfterEachMethod(CompilerConfiguration options) {
        options.addCompilationCustomizers(new AddValidationAfterEachMethodCompilationCustomizer())

        // debug - print out resulting script:
        // options.addCompilationCustomizers(new LoggingCompilationCustomizer())
    }

    @TypeChecked(TypeCheckingMode.SKIP)
    private static void addExtraCustomizers(CompilerConfiguration cc, List<CompilationCustomizer> extraCompilationCustomizers)
    {
        if (extraCompilationCustomizers) {
            cc.addCompilationCustomizers(*extraCompilationCustomizers);
        }
    }

    @CompileStatic
    protected GroovyShell constructParser(Class c, List<CompilationCustomizer> extraCompilationCustomizers = []) {
        def compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.scriptBaseClass = c.name

        restrictScript(compilerConfiguration)
        makePrivatePublic(compilerConfiguration)
        validateAfterEachMethod(compilerConfiguration)
        addExtraCustomizers(compilerConfiguration, extraCompilationCustomizers)

        return new GroovyShell(
                new SandboxClassLoader(c.classLoader),
                new DoNotCallMeBinding(),
                compilerConfiguration);
    }

    boolean hasAnyOfFlags(Set<Flags> flags) {
        return this.flags.intersect(flags)
    }

    String patchScriptText(String scriptText) {
        scriptText = scriptText.replaceAll("new\\s*Date\\s*\\(\\s*\\)", "me.biocomp.hubitat_ci.util.integration.TimeKeeper.now()")
        return scriptText
    }
}
