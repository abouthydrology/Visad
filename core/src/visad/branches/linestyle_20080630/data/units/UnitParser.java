/*
 * Copyright 1999, University Corporation for Atmospheric Research
 * See file LICENSE for copying and redistribution conditions.
 */

/* Generated By:JavaCC: Do not edit this line. UnitParser.java */
    package visad.data.units;

    import java.io.ByteArrayInputStream;
    import java.io.InputStreamReader;
    import java.io.LineNumberReader;
    import java.util.StringTokenizer;
    import visad.DerivedUnit;
    import visad.SI;
    import visad.ScaledUnit;
    import visad.Unit;
    import visad.UnitException;

    public class UnitParser implements UnitParserConstants {
        /**
         * The units database.
         */
        protected static UnitsDB        unitsDB = null;

        static
        {
            try
            {
                unitsDB = DefaultUnitsDB.instance();
            }
            catch(UnitException e)
            {
            }
        }

        /**
         * The canonical time unit.
         */
        protected static final Unit     second = SI.second;

        /**
         * Whether or not we're decoding a time unit.
         */
        protected boolean               isTime;

        /**
         * The Julian day number of the (artificial) time origin.
         */
        protected static final long     julianDayOrigin =
                                            UnitParser.julianDay(2001, 1, 1);


        /**
         * Compute the Julian day number of a date.
         */
        public static long
        julianDay(int year, int month, int day)
        {
            long        igreg = 15 + 31 * (10 + (12 * 1582));
            int         iy;     // signed, origin-0 year
            int         ja;     // Julian century
            int         jm;     // Julian month
            int         jy;     // Julian year
            long        julday; // returned Julian day number

            /*
             * Because there is no 0 BC or 0 AD, assume the user wants
             * the start of the common era if they specify year 0.
             */
            if (year == 0)
                year = 1;

            iy = year;
            if (year < 0)
                iy++;
            if (month > 2)
            {
                jy = iy;
                jm = month + 1;
            }
            else
            {
                jy = iy - 1;
                jm = month + 13;

            }

            julday = day + (int)(30.6001 * jm);
            if (jy >= 0)
            {
                julday += 365 * jy;
                julday += 0.25 * jy;
            }
            else
            {
                double  xi = 365.25 * jy;

                if ((int)xi != xi)
                    xi -= 1;
                julday += (int)xi;
            }
            julday += 1720995;

            if (day + (31* (month + (12 * iy))) >= igreg)
            {
                ja = jy/100;
                julday -= ja;
                julday += 2;
                julday += ja/4;
            }

            return julday;
        }


        /**
         * Encode a timestamp as a double value in units of seconds.
         */
        public static double
        encodeTimestamp(int year, int month, int day,
            int hour, int minute, float second, int zone)
        {
            return (julianDay(year, month, day) - julianDayOrigin) *
                86400.0 + (hour*60 + minute - zone)*60 + second;
        }


        /**
         * Test this class.
         */
        public static void main(String[] args)
            throws Exception
        {
            UnitParser          parser = new UnitParser(System.in);
            LineNumberReader    lineInput = new LineNumberReader(
                                    new InputStreamReader(System.in));

            for (;;)
            {
                System.out.print("Enter a unit specification or ^D to quit: ");

                String  spec = lineInput.readLine();
                if (spec == null)
                    break;

                spec = spec.trim();

                if (spec.length() > 0)
                {
                    parser.ReInit(new ByteArrayInputStream(spec.getBytes()));

                    try
                    {
                        Unit    unit = parser.unitSpec();
                        System.out.println("unit = " + unit);
                        System.out.println(
                            "definition = " + unit.getDefinition());
                    }
                    catch (ParseException e)
                    {
                        System.out.println(e.getMessage());
                    }
                }
            }
            System.out.println("");
        }

  final public Unit unitSpec() throws ParseException {
    Unit        unit = null;
    double      origin = 0;
    boolean     originSpecified = false;

    isTime = false;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case INTEGER:
    case REAL:
    case NAME:
    case 23:
    case 25:
      unit = unitProductList();
            isTime = Unit.canConvert(unit, second);
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case SHIFT:
        origin = shiftExpression();
                originSpecified = true;
        break;
      default:
        jj_la1[0] = jj_gen;
        ;
      }
      break;
    default:
      jj_la1[1] = jj_gen;
      ;
    }
    jj_consume_token(0);
        try
        {
            if (unit == null)
                unit = new DerivedUnit();       // dimensionless derived unit
            if (originSpecified && isTime)
                unit = unit.shift(unit.toThis(origin, second));
            else if (origin != 0)
                unit = unit.shift(origin);
            {if (true) return unit;}
        }
        catch (UnitException e)
        {
            {if (true) throw new ParseException("Invalid unit specification: " +
                e.getMessage());}
        }
    throw new Error("Missing return statement in function");
  }

  final public Unit unitProductList() throws ParseException {
    double      value;
    Unit        unit1, unit2;
    unit1 = powerExpression();
    label_1:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
      case REAL:
      case WHITESPACE:
      case DIVIDE:
      case NAME:
      case 23:
      case 24:
      case 25:
        ;
        break;
      default:
        jj_la1[2] = jj_gen;
        break label_1;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
      case REAL:
      case WHITESPACE:
      case NAME:
      case 23:
      case 24:
      case 25:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case WHITESPACE:
        case 23:
        case 24:
          multiply();
          break;
        default:
          jj_la1[3] = jj_gen;
          ;
        }
        unit2 = powerExpression();
                try
                {
                    unit1 = unit1.multiply(unit2);
                }
                catch (UnitException e)
                {
                    {if (true) throw new ParseException("Couldn't multiply units");}
                }
        break;
      case DIVIDE:
        jj_consume_token(DIVIDE);
        unit2 = powerExpression();
                try
                {
                    unit1 = unit1.divide(unit2);
                }
                catch (UnitException e)
                {
                    {if (true) throw new ParseException("Couldn't divide units");}
                }
        break;
      default:
        jj_la1[4] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
        {if (true) return unit1;}
    throw new Error("Missing return statement in function");
  }

  final public void multiply() throws ParseException {
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case 23:
      jj_consume_token(23);
      break;
    case 24:
      jj_consume_token(24);
      break;
    case WHITESPACE:
      jj_consume_token(WHITESPACE);
      break;
    default:
      jj_la1[5] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
  }

  final public Unit powerExpression() throws ParseException {
    double      value;
    Unit        unit;
    Token       t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case INTEGER:
    case REAL:
    case 23:
      value = numberExpression();
                unit=new ScaledUnit(value);
      break;
    case NAME:
    case 25:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case NAME:
        unit = nameExpression();
        break;
      case 25:
        jj_consume_token(25);
        unit = unitProductList();
        jj_consume_token(26);
        break;
      default:
        jj_la1[6] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
      case 27:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case 27:
          jj_consume_token(27);
          break;
        default:
          jj_la1[7] = jj_gen;
          ;
        }
        t = jj_consume_token(INTEGER);
                    try
                    {
                        // must use parseFloat in case we have "+00" which
                        // causes parseInt to bomb
                        unit = unit.pow((int) Float.parseFloat(t.image));
                        //unit = unit.pow(Integer.parseInt(t.image));
                    }
                    catch (UnitException e)
                    {
                        {if (true) throw new ParseException(
                            "Couldn't raise unit to a power");}
                    }
        break;
      default:
        jj_la1[8] = jj_gen;
        ;
      }
      break;
    default:
      jj_la1[9] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
        {if (true) return unit;}
    throw new Error("Missing return statement in function");
  }

  final public Unit nameExpression() throws ParseException {
    Token       t;
    Unit        unit;
    t = jj_consume_token(NAME);
        unit = unitsDB.get(t.image);
        if (unit == null)
        {
            {if (true) throw new NoSuchUnitException("Unit not in database");}
        }
        {if (true) return unit;}
    throw new Error("Missing return statement in function");
  }

  final public double numberExpression() throws ParseException {
    double      value;
    Token       t;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case INTEGER:
      t = jj_consume_token(INTEGER);
                value = Integer.parseInt(t.image);
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case 23:
        jj_consume_token(23);
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case INTEGER:
          // default algorithm is OK
                              t = jj_consume_token(INTEGER);
                        value += new Double("." + t.image).doubleValue();
                        if (value < 0)
                            {if (true) throw new ParseException(
                                "negative sign follows decimal point");}
          break;
        default:
          jj_la1[10] = jj_gen;
          ;
        }
        break;
      default:
        jj_la1[11] = jj_gen;
        ;
      }
      break;
    case 23:
      jj_consume_token(23);
      t = jj_consume_token(INTEGER);
                value = new Double("." + t.image).doubleValue();
                if (value < 0)
                    {if (true) throw new ParseException(
                        "negative sign follows decimal point");}
      break;
    case REAL:
      t = jj_consume_token(REAL);
                // Double.parseDouble() *should* exist but doesn't (sigh).
                value = new Double(t.image).doubleValue();
      break;
    default:
      jj_la1[12] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
        {if (true) return value;}
    throw new Error("Missing return statement in function");
  }

  final public double shiftExpression() throws ParseException {
    double      origin;
    jj_consume_token(SHIFT);
    if (jj_2_1(3)) {
      origin = timestampExpression();
                if (!isTime)
                    {if (true) throw new ParseException("non-time unit with timestamp");}
                {if (true) return origin;}
    } else {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
      case REAL:
      case 23:
      case 25:
        origin = valueExpression();
                {if (true) return origin;}
        break;
      default:
        jj_la1[13] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
    throw new Error("Missing return statement in function");
  }

  final public double valueExpression() throws ParseException {
    double      value;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case INTEGER:
    case REAL:
    case 23:
      value = numericalTerm();
      break;
    case 25:
      jj_consume_token(25);
      value = valueExpression();
      jj_consume_token(26);
      break;
    default:
      jj_la1[14] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
        {if (true) return value;}
    throw new Error("Missing return statement in function");
  }

  final public double numericalTerm() throws ParseException {
    double      value1, value2;
    value1 = numberExpression();
    label_2:
    while (true) {
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
      case REAL:
      case DIVIDE:
      case 23:
        ;
        break;
      default:
        jj_la1[15] = jj_gen;
        break label_2;
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
      case REAL:
      case 23:
        value2 = numberExpression();
                value1 *= value2;
        break;
      case DIVIDE:
        jj_consume_token(DIVIDE);
        value2 = numberExpression();
                value1 /= value2;
        break;
      default:
        jj_la1[16] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
    }
        {if (true) return value1;}
    throw new Error("Missing return statement in function");
  }

  final public double timestampExpression() throws ParseException {
    double      value;
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case DATE:
      value = timestampTerm();
      break;
    case 25:
      jj_consume_token(25);
      value = timestampExpression();
      jj_consume_token(26);
      break;
    default:
      jj_la1[17] = jj_gen;
      jj_consume_token(-1);
      throw new ParseException();
    }
        {if (true) return value;}
    throw new Error("Missing return statement in function");
  }

  final public double timestampTerm() throws ParseException {
    int         year = 0;
    int         month = 0;
    int         day = 0;
    int         hour = 0;
    int         minute = 0;
    int         zone = 0;       // time zone in minutes
    float       second = 0;
    double      when = 0;
    Token       t;
    int         zoneHour;
    int         zoneMinute;
    t = jj_consume_token(DATE);
        StringTokenizer dateSpec = new StringTokenizer(t.image, "-");

        year = Integer.parseInt(dateSpec.nextToken());
        month = Integer.parseInt(dateSpec.nextToken());
        day = Integer.parseInt(dateSpec.nextToken());
    switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
    case INTEGER:
    case WHITESPACE:
    case NAME:
    case TIME:
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case NAME:
        t = jj_consume_token(NAME);
                    if (!(t.image.equalsIgnoreCase("T")))
                        {if (true) throw new ParseException("invalid timestamp");}
        break;
      default:
        jj_la1[19] = jj_gen;
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case WHITESPACE:
          jj_consume_token(WHITESPACE);
          break;
        default:
          jj_la1[18] = jj_gen;
          ;
        }
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
        t = jj_consume_token(INTEGER);
                    hour = Integer.parseInt(t.image);
        break;
      case TIME:
        t = jj_consume_token(TIME);
                    StringTokenizer     timeSpec =
                        new StringTokenizer(t.image, ":");

                    hour = Integer.parseInt(timeSpec.nextToken());
                    minute = Integer.parseInt(timeSpec.nextToken());
                    if (timeSpec.hasMoreTokens())
                        second = new Float(timeSpec.nextToken()).floatValue();
        break;
      default:
        jj_la1[20] = jj_gen;
        jj_consume_token(-1);
        throw new ParseException();
      }
      switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
      case INTEGER:
      case WHITESPACE:
      case NAME:
      case TIME:
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case WHITESPACE:
          jj_consume_token(WHITESPACE);
          break;
        default:
          jj_la1[21] = jj_gen;
          ;
        }
        switch ((jj_ntk==-1)?jj_ntk():jj_ntk) {
        case INTEGER:
          t = jj_consume_token(INTEGER);
                        zoneMinute = 0;

                        zoneHour = (int)Float.parseFloat(t.image);

                        if (zoneHour <= -100 || zoneHour >= 100)
                        {
                            zoneMinute = zoneHour % 100;
                            zoneHour /= 100;
                        }

                        zone = zoneHour * 60 + zoneMinute;
          break;
        case TIME:
          t = jj_consume_token(TIME);
                        StringTokenizer zoneSpec =
                            new StringTokenizer(t.image, ":");
                        int     sign = t.image.startsWith("-") ? -1 : 1;

                        // must use parseFloat in case we have "+00" which
                        // causes parseInt to bomb
                        zoneHour = (int) Float.parseFloat(zoneSpec.nextToken());
                        zoneMinute = Integer.parseInt(zoneSpec.nextToken());

                        zone = zoneHour*60 + zoneMinute*sign;
          break;
        case NAME:
          t = jj_consume_token(NAME);
                        if (!t.image.equals("UTC") &&
                            !t.image.equals("GMT") &&
                            !t.image.equals("ZULU") &&
                            !t.image.equals("Z"))
                        {
                            {if (true) throw new ParseException("invalid time zone");}
                        }
          break;
        default:
          jj_la1[22] = jj_gen;
          jj_consume_token(-1);
          throw new ParseException();
        }
        break;
      default:
        jj_la1[23] = jj_gen;
        ;
      }
      break;
    default:
      jj_la1[24] = jj_gen;
      ;
    }
        if (month < 1 || month > 12 ||
            day < 1 || day > 31 ||
            hour < 0 || hour > 23 ||
            minute < 0 || minute > 59 ||
            second < 0 || second > 61 ||
            zone < -1440 || zone > 1440)
        {
            {if (true) throw new ParseException("invalid timestamp");}
        }

        {if (true) return UnitParser.encodeTimestamp(year, month, day,
            hour, minute, second, zone);}
    throw new Error("Missing return statement in function");
  }

  final private boolean jj_2_1(int xla) {
    jj_la = xla; jj_lastpos = jj_scanpos = token;
    try { return !jj_3_1(); }
    catch(LookaheadSuccess ls) { return true; }
    finally { jj_save(0, xla); }
  }

  final private boolean jj_3R_14() {
    if (jj_scan_token(TIME)) return true;
    return false;
  }

  final private boolean jj_3R_6() {
    if (jj_scan_token(DATE)) return true;
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_7()) jj_scanpos = xsp;
    return false;
  }

  final private boolean jj_3R_8() {
    if (jj_scan_token(NAME)) return true;
    return false;
  }

  final private boolean jj_3R_12() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_scan_token(8)) jj_scanpos = xsp;
    xsp = jj_scanpos;
    if (jj_3R_13()) {
    jj_scanpos = xsp;
    if (jj_3R_14()) {
    jj_scanpos = xsp;
    if (jj_3R_15()) return true;
    }
    }
    return false;
  }

  final private boolean jj_3R_3() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_4()) {
    jj_scanpos = xsp;
    if (jj_3R_5()) return true;
    }
    return false;
  }

  final private boolean jj_3R_15() {
    if (jj_scan_token(NAME)) return true;
    return false;
  }

  final private boolean jj_3R_11() {
    if (jj_scan_token(TIME)) return true;
    return false;
  }

  final private boolean jj_3R_7() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_3R_8()) {
    jj_scanpos = xsp;
    if (jj_3R_9()) return true;
    }
    xsp = jj_scanpos;
    if (jj_3R_10()) {
    jj_scanpos = xsp;
    if (jj_3R_11()) return true;
    }
    xsp = jj_scanpos;
    if (jj_3R_12()) jj_scanpos = xsp;
    return false;
  }

  final private boolean jj_3_1() {
    if (jj_3R_3()) return true;
    return false;
  }

  final private boolean jj_3R_13() {
    if (jj_scan_token(INTEGER)) return true;
    return false;
  }

  final private boolean jj_3R_10() {
    if (jj_scan_token(INTEGER)) return true;
    return false;
  }

  final private boolean jj_3R_5() {
    if (jj_scan_token(25)) return true;
    if (jj_3R_3()) return true;
    if (jj_scan_token(26)) return true;
    return false;
  }

  final private boolean jj_3R_4() {
    if (jj_3R_6()) return true;
    return false;
  }

  final private boolean jj_3R_9() {
    Token xsp;
    xsp = jj_scanpos;
    if (jj_scan_token(8)) jj_scanpos = xsp;
    return false;
  }

  public UnitParserTokenManager token_source;
  SimpleCharStream jj_input_stream;
  public Token token, jj_nt;
  private int jj_ntk;
  private Token jj_scanpos, jj_lastpos;
  private int jj_la;
  public boolean lookingAhead = false;
  private boolean jj_semLA;
  private int jj_gen;
  final private int[] jj_la1 = new int[25];
  static private int[] jj_la1_0;
  static {
      jj_la1_0();
   }
   private static void jj_la1_0() {
      jj_la1_0 = new int[] {0x800,0x2804090,0x3805190,0x1800100,0x3805190,0x1800100,0x2004000,0x8000000,0x8000010,0x2804090,0x10,0x800000,0x800090,0x2800090,0x2800090,0x801090,0x801090,0x2040000,0x100,0x4000,0x400010,0x100,0x404010,0x404110,0x404110,};
   }
  final private JJCalls[] jj_2_rtns = new JJCalls[1];
  private boolean jj_rescan = false;
  private int jj_gc = 0;

  public UnitParser(java.io.InputStream stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new UnitParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 25; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(java.io.InputStream stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 25; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public UnitParser(java.io.Reader stream) {
    jj_input_stream = new SimpleCharStream(stream, 1, 1);
    token_source = new UnitParserTokenManager(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 25; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(java.io.Reader stream) {
    jj_input_stream.ReInit(stream, 1, 1);
    token_source.ReInit(jj_input_stream);
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 25; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public UnitParser(UnitParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 25; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  public void ReInit(UnitParserTokenManager tm) {
    token_source = tm;
    token = new Token();
    jj_ntk = -1;
    jj_gen = 0;
    for (int i = 0; i < 25; i++) jj_la1[i] = -1;
    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();
  }

  final private Token jj_consume_token(int kind) throws ParseException {
    Token oldToken;
    if ((oldToken = token).next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    if (token.kind == kind) {
      jj_gen++;
      if (++jj_gc > 100) {
        jj_gc = 0;
        for (int i = 0; i < jj_2_rtns.length; i++) {
          JJCalls c = jj_2_rtns[i];
          while (c != null) {
            if (c.gen < jj_gen) c.first = null;
            c = c.next;
          }
        }
      }
      return token;
    }
    token = oldToken;
    jj_kind = kind;
    throw generateParseException();
  }

  static private final class LookaheadSuccess extends java.lang.Error { }
  final private LookaheadSuccess jj_ls = new LookaheadSuccess();
  final private boolean jj_scan_token(int kind) {
    if (jj_scanpos == jj_lastpos) {
      jj_la--;
      if (jj_scanpos.next == null) {
        jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
      } else {
        jj_lastpos = jj_scanpos = jj_scanpos.next;
      }
    } else {
      jj_scanpos = jj_scanpos.next;
    }
    if (jj_rescan) {
      int i = 0; Token tok = token;
      while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
      if (tok != null) jj_add_error_token(kind, i);
    }
    if (jj_scanpos.kind != kind) return true;
    if (jj_la == 0 && jj_scanpos == jj_lastpos) throw jj_ls;
    return false;
  }

  final public Token getNextToken() {
    if (token.next != null) token = token.next;
    else token = token.next = token_source.getNextToken();
    jj_ntk = -1;
    jj_gen++;
    return token;
  }

  final public Token getToken(int index) {
    Token t = lookingAhead ? jj_scanpos : token;
    for (int i = 0; i < index; i++) {
      if (t.next != null) t = t.next;
      else t = t.next = token_source.getNextToken();
    }
    return t;
  }

  final private int jj_ntk() {
    if ((jj_nt=token.next) == null)
      return (jj_ntk = (token.next=token_source.getNextToken()).kind);
    else
      return (jj_ntk = jj_nt.kind);
  }

  private java.util.Vector jj_expentries = new java.util.Vector();
  private int[] jj_expentry;
  private int jj_kind = -1;
  private int[] jj_lasttokens = new int[100];
  private int jj_endpos;

  private void jj_add_error_token(int kind, int pos) {
    if (pos >= 100) return;
    if (pos == jj_endpos + 1) {
      jj_lasttokens[jj_endpos++] = kind;
    } else if (jj_endpos != 0) {
      jj_expentry = new int[jj_endpos];
      for (int i = 0; i < jj_endpos; i++) {
        jj_expentry[i] = jj_lasttokens[i];
      }
      boolean exists = false;
      for (java.util.Enumeration e = jj_expentries.elements(); e.hasMoreElements();) {
        int[] oldentry = (int[])(e.nextElement());
        if (oldentry.length == jj_expentry.length) {
          exists = true;
          for (int i = 0; i < jj_expentry.length; i++) {
            if (oldentry[i] != jj_expentry[i]) {
              exists = false;
              break;
            }
          }
          if (exists) break;
        }
      }
      if (!exists) jj_expentries.addElement(jj_expentry);
      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;
    }
  }

  public ParseException generateParseException() {
    jj_expentries.removeAllElements();
    boolean[] la1tokens = new boolean[28];
    for (int i = 0; i < 28; i++) {
      la1tokens[i] = false;
    }
    if (jj_kind >= 0) {
      la1tokens[jj_kind] = true;
      jj_kind = -1;
    }
    for (int i = 0; i < 25; i++) {
      if (jj_la1[i] == jj_gen) {
        for (int j = 0; j < 32; j++) {
          if ((jj_la1_0[i] & (1<<j)) != 0) {
            la1tokens[j] = true;
          }
        }
      }
    }
    for (int i = 0; i < 28; i++) {
      if (la1tokens[i]) {
        jj_expentry = new int[1];
        jj_expentry[0] = i;
        jj_expentries.addElement(jj_expentry);
      }
    }
    jj_endpos = 0;
    jj_rescan_token();
    jj_add_error_token(0, 0);
    int[][] exptokseq = new int[jj_expentries.size()][];
    for (int i = 0; i < jj_expentries.size(); i++) {
      exptokseq[i] = (int[])jj_expentries.elementAt(i);
    }
    return new ParseException(token, exptokseq, tokenImage);
  }

  final public void enable_tracing() {
  }

  final public void disable_tracing() {
  }

  final private void jj_rescan_token() {
    jj_rescan = true;
    for (int i = 0; i < 1; i++) {
      JJCalls p = jj_2_rtns[i];
      do {
        if (p.gen > jj_gen) {
          jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
          switch (i) {
            case 0: jj_3_1(); break;
          }
        }
        p = p.next;
      } while (p != null);
    }
    jj_rescan = false;
  }

  final private void jj_save(int index, int xla) {
    JJCalls p = jj_2_rtns[index];
    while (p.gen > jj_gen) {
      if (p.next == null) { p = p.next = new JJCalls(); break; }
      p = p.next;
    }
    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
  }

  static final class JJCalls {
    int gen;
    Token first;
    int arg;
    JJCalls next;
  }

    }