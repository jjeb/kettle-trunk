 /**********************************************************************
 **                                                                   **
 **               This code belongs to the KETTLE project.            **
 **                                                                   **
 ** Kettle, from version 2.2 on, is released into the public domain   **
 ** under the Lesser GNU Public License (LGPL).                       **
 **                                                                   **
 ** For more details, please read the document LICENSE.txt, included  **
 ** in this project                                                   **
 **                                                                   **
 ** http://www.kettle.be                                              **
 ** info@kettle.be                                                    **
 **                                                                   **
 **********************************************************************/
 
package be.ibridge.kettle.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigDecimal;
import java.util.Date;

import junit.framework.TestCase;
import be.ibridge.kettle.core.exception.KettleFileException;
import be.ibridge.kettle.core.value.Value;

/**
 * Test class for the basic functionality of Row.
 *
 * Rows on the same stream in KETTLE are always assumed to be
 * of the same width.
 *
 * @author Sven Boden
 */
public class RowTest extends TestCase
{
	/*
	 * Constructor test 1. No params.
	public void testConstructor1()
	{
	    Row r = new Row();
	    assertTrue(!r.isIgnored());
	    assertNull(r.getLogdate());
	    assertEquals(0L, r.getLogtime());

	    r.setLogdate();
		assertTrue(r.getLogtime() != 0L);    
	}
     */

	/**
	 * Constructor test 2. Several cases.
	 */
	public void testConstructor2()
	{
		Row rEmpty = new Row();

	    Row r1 = new Row();
        r1.addValue(new Value("field1", "KETTLE"));
        r1.addValue(new Value("field2", 123L));
        r1.addValue(new Value("field3", true));
        r1.setIgnore();

        Row r2 = new Row(r1);
        Row r3 = new Row(rEmpty);

        assertEquals(0, rEmpty.size());
        assertEquals(r1.size(), r2.size());
        assertTrue(r1.equals(r2));
        assertTrue(r1.isIgnored());
        assertEquals(0, r3.size());
	}

	/**
	 * Test RemoveValue().
	 */
	public void testRemoveValue()
	{
		Value values[] = {
		    new Value("field1", "KETTLE"),                // String
			new Value("field2", 123L),                    // integer
			new Value("field3", 10.5D),                   // double
			new Value("field4", new Date()),              // Date
			new Value("field5", true),                    // Boolean
			new Value("field6", new BigDecimal(123.45)),  // BigDecimal
			new Value("field6", new BigDecimal(123.60))   // BigDecimal
		};

	    Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}
		assertEquals(values.length, r1.size());


		r1.removeValue(0);
		assertEquals(values.length - 1, r1.size());
		assertEquals(values[1], r1.getValue(0));

		boolean removed;
		int idx;

		idx = r1.searchValueIndex("field5");
		assertTrue(idx > 0);
		removed = r1.removeValue("field5");
		idx = r1.searchValueIndex("field5");
		assertTrue(removed);
		assertTrue(idx < 0);
		assertEquals(values.length - 2, r1.size());

		// RemoveValue and duplicate values
		idx = r1.searchValueIndex("field6");
		assertTrue(idx > 0);
		removed = r1.removeValue("field6");
		idx = r1.searchValueIndex("field6");
		assertTrue(removed);
		assertTrue(idx > 0);
		assertEquals(values.length - 3, r1.size());

		idx = r1.searchValueIndex("field6");
		assertTrue(idx > 0);
		removed = r1.removeValue("field6");
		idx = r1.searchValueIndex("field6");
		assertTrue(removed);
		assertTrue(idx < 0);
		assertEquals(values.length - 4, r1.size());

		removed = r1.removeValue("field6");
		assertTrue(!removed);
	}

	/**
	 * Test clear().
	 */
	public void testClear()
	{
		Value values[] = {
		    new Value("field1", "KETTLE"),                // String
			new Value("field2", 123L),                    // integer
			new Value("field3", 10.5D),                   // double
			new Value("field4", new Date()),              // Date
			new Value("field5", true),                    // Boolean
			new Value("field6", new BigDecimal(123.45))   // BigDecimal
		};

	    Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}
		assertEquals(values.length, r1.size());

		r1.clear();
		assertEquals(0, r1.size());

		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}
		assertEquals(values.length, r1.size());
	}

	/**
	 * Test addValue().
	 */
	public void testAddValue()
	{
		Value values[] = {
		    new Value("field1", "KETTLE"),                // String
			new Value("field2", 123L),                    // integer
			new Value("field3", 10.5D),                   // double
			new Value("field4", new Date()),              // Date
			new Value("field5", true),                    // Boolean
			new Value("field6", new BigDecimal(123.45))   // BigDecimal
		};

		Value values1[] = {
			new Value("field1", "dupl"),                  // String
			new Value("field2", "string"),                // String
			new Value("field7", true),                    // Boolean
     	};

	    Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}
		assertEquals(values.length, r1.size());

	    Row r2 = new Row();
		for (int i=0; i < values1.length; i++ )
		{
			r2.addValue(values1[i]);
		}
		assertEquals(values1.length, r2.size());

		r1.addRow(r2);
		assertEquals(values.length + values1.length, r1.size());

		Value v1 = r1.getValue(values.length);
		assertEquals(values1[0], v1);

		Value v2 = r1.getValue(values.length+1);
		assertEquals(values1[1], v2);

		Value v3 = r1.getValue(values.length+2);
		assertEquals(values1[2], v3);
		
		// Add some values to a row
		Row r3 = new Row();
		for (int i=0; i < values1.length; i++ )
		{
			r3.addValue(values1[i]);
		}
		
		// Add at a certain index
		r3.addValue(3, new Value("new3", true));
		r3.addValue(1, new Value("new2", true));
		r3.addValue(0, new Value("new1", true));
		assertEquals(0, r3.searchValueIndex("new1"));
		assertEquals(2, r3.searchValueIndex("new2"));
		assertEquals(5, r3.searchValueIndex("new3"));
	}

	/**
	 * Test mergeRow().
	 */
	public void testMergeRow()
	{
		Value values[] = {
		    new Value("field1", "KETTLE"),                // String
			new Value("field2", 123L),                    // integer
			new Value("field3", 10.5D),                   // double
			new Value("field4", new Date()),              // Date
			new Value("field5", true),                    // Boolean
			new Value("field6", new BigDecimal(123.45))   // BigDecimal
		};

		Value values1[] = {
			new Value("field1", "dupl"),                  // String
			new Value("field2", "string"),                // String
			new Value("field7", true),                    // Boolean
     	};

	    Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}
		assertEquals(values.length, r1.size());

	    Row r2 = new Row();
		for (int i=0; i < values1.length; i++ )
		{
			r2.addValue(values1[i]);
		}
		assertEquals(values1.length, r2.size());

		r1.mergeRow(r2);
		assertEquals(values.length + 1, r1.size());

		Value v1 = r1.getValue(values.length);
		assertEquals(values1[2], v1);
	}

	/**
	 * Test getValue.
	 */
	public void testGetValue()
	{
		Value values[] = {
		    new Value("field1", "KETTLE"),                // String
			new Value("field2", 123L),                    // integer
			new Value("field3", 10.5D),                   // double
			new Value("field4", new Date()),              // Date
			new Value("field5", true),                    // Boolean
			new Value("field6", new BigDecimal(123.45))   // BigDecimal
		};

	    Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}

		for (int i=0; i < values.length; i++ )
		{
			Value v = r1.getValue(i);
			assertTrue(v.equals(values[i]));
		}

		//////////////////////////////////////////////////////////
		// If you're here because the build fails and you want
		// to change the following part, think again (Sven Boden).
		//
		// A Row returns an IndexOutOfBoundsException when the
		// requested value is not in the Row. If this would be
		// catched and a null returned, this check would slow
		// down processing a lot.
		//
		// It's also unusual to get it in KETTLE, as one of the
		// assumptions is that all Rows on a certain stream are
		// of the same width (same number of columns).
		//////////////////////////////////////////////////////////
		try  {
		    r1.getValue(values.length + 1);
		    fail("expected out of bounds exception");
		}
		catch ( IndexOutOfBoundsException e ) {	
		}
	}

	/**
	 * Test searchValueIndex.
	 */
	public void testSearchValueIndex()
	{
		Date date = new Date();

		Value values[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", date),                    // Date
				new Value("field5", true),                    // Boolean
				new Value("field6", new BigDecimal(123.45)),  // BigDecimal
		};

		Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}

		int idx;
		idx = r1.searchValueIndex("field0");
		assertEquals(-1L, idx);

		idx = r1.searchValueIndex("field1");
		assertEquals(0, idx);

		idx = r1.searchValueIndex("field6");
		assertEquals(5, idx);

		Row r2 = new Row(r1);

		Value values1[] = {
  		    new Value("field8", "KETTLE "),               // String
		    new Value("field8", "KETTLE1"),               // String
		    new Value("field9 ", "KETTLE1")               // String
		};

		for (int i=0; i < values1.length; i++ )
		{
			r2.addValue(values1[i]);
		}

		idx = r2.searchValueIndex("field8");
		assertEquals(6, idx);

		idx = r2.searchValueIndex("field8");
		assertEquals(6, idx);

		idx = r2.searchValueIndex("field9 ");
		assertEquals(8, idx);
	}

	/**
	 * Test searchValue.
	 */
	public void testSearchValue()
	{
		Date date = new Date();

		Value values[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", date),                    // Date
				new Value("field5", true),                    // Boolean
				new Value("field6", new BigDecimal(123.45)),  // BigDecimal
		};

		Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}

		Value v0 = r1.searchValue("field0");
		assertNull(v0);

		Value v1 = r1.searchValue("field1");
		assertEquals("field1", v1.getName());
		assertEquals("KETTLE", v1.getString());

		Value v2 = r1.searchValue("field2");
		assertEquals("field2", v2.getName());
		assertEquals(new Long(123L), new Long(v2.getInteger()));

		Value v3 = r1.searchValue("field3");
		assertEquals("field3", v3.getName());
		assertEquals(new Double(10.5D), new Double(v3.getNumber()));

		Value v4 = r1.searchValue("field4");
		assertEquals("field4", v4.getName());
		assertEquals(date, v4.getDate());

		Value v5 = r1.searchValue("field5");
		assertEquals("field5", v5.getName());
		assertEquals(true, v5.getBoolean());

		Value v6 = r1.searchValue("field6");
		assertEquals("field6", v6.getName());
		assertEquals(new BigDecimal(123.45), v6.getBigNumber());

		Row r2 = new Row(r1);

		Value values1[] = {
  		    new Value("field8", "KETTLE "),               // String
		    new Value("field8", "KETTLE1"),               // String
		    new Value("field9 ", "KETTLE1")               // String
		};

		for (int i=0; i < values1.length; i++ )
		{
			r2.addValue(values1[i]);
		}

		Value v21 = r2.searchValue("field8");
		assertEquals("field8", v21.getName());
		assertEquals("KETTLE ", v21.getString());

		/* Second value is never found */
		Value v22 = r2.searchValue("field8");
		assertEquals("field8", v22.getName());
		assertEquals("KETTLE ", v22.getString());

		Value v23 = r2.searchValue("field9 ");
		assertEquals("field9 ", v23.getName());
		assertEquals("KETTLE1", v23.getString());
	}

	/**
	 * Test isEmpty.
	 */
	public void testIsEmpty()
	{
		Row r0 = new Row();

	    Row r1 = new Row();
        r1.addValue(new Value("field1", "KETTLE"));
        r1.addValue(new Value("field2", 123L));
        r1.addValue(new Value("field3", true));

        Row r2 = new Row();
        r2.addValue(new Value("field1", (String)null));
        r2.addValue(new Value("field2", (String)null));

        Row r3 = new Row();
        r3.addValue(new Value("field1", "KETTLE"));
        r3.addValue(new Value("field2", (String)null));

        assertTrue(r0.isEmpty());            /* row with no value is empty */
        assertFalse(r1.isEmpty());			 /* normal row */
        assertTrue(r2.isEmpty());			 /* all values are NULL => empty */
        assertFalse(r3.isEmpty());           /* some values are non null => not empty */
	}

	/**
	 * Test getFieldNames.
	 */
	public void testGetFieldNames()
	{
		Row r0 = new Row();

		Value values[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", new Date()),              // Date
				new Value("field5", true),                    // Boolean
				new Value("field6", new BigDecimal(123.45))   // BigDecimal
		};

		Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}

		String fieldNames[] = r1.getFieldNames();
		for (int i=0; i < fieldNames.length; i++ )
		{
			assertEquals(fieldNames[i], values[i].getName());
		}
		assertEquals(fieldNames.length, values.length);

		String fieldNames1[] = r0.getFieldNames();
		assertEquals(0L, fieldNames1.length);
	}

	/**
	 * Test toString().
	 */
	public void testToString()
	{
		Row r0 = new Row();

		Value values[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", true),                    // Boolean
				new Value("field5", new BigDecimal(123.0)),   // BigDecimal
				new Value("field6", (String)null),            // NULL value
				null                                          // null
		};

		Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}

		assertEquals("[]", r0.toString());
		assertEquals("[field1=KETTLE, field2= 123, field3=10.5, field4=true, field5=123, field6=, NULL]", r1.toString());
	}

	/**
	 * Test the getXXX() methods.
	 */
	public void testGetValues()
	{
		Value values[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", true),                    // Boolean
				new Value("field5", new BigDecimal(123.0)),   // BigDecimal
				new Value("field6", (String)null),            // NULL value
				new Value("field7", new Date(10000000L)),     // Date
		};

		Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}

		assertEquals(true,  r1.getBoolean("field4", false));
		assertEquals(true,  r1.getBoolean("field3", false));
		assertEquals(false, r1.getBoolean("unknown", false));
		assertEquals(true,  r1.getBoolean("unknown", true));

		assertEquals("KETTLE",  r1.getString("field1", "default"));
		assertEquals("Y",       r1.getString("field4", "default"));
		assertEquals("default", r1.getString("unknown", "default"));

		assertEquals(123.0D, r1.getNumber("field2", 100.0D), 0.1D);
		assertEquals(1.0D,   r1.getNumber("field4", 100.0D), 0.1D);
		assertEquals(100.0D, r1.getNumber("unknown", 100.0D), 0.1D);

		assertEquals(123L, r1.getInteger("field2", 100L));
		assertEquals(1L,   r1.getInteger("field4", 100L));
		assertEquals(100L, r1.getInteger("unknown", 100L));

		assertEquals(123, r1.getShort("field2", 100));
		assertEquals(1,   r1.getShort("field4", 100));
		assertEquals(100, r1.getShort("unknown", 100));
		
		assertEquals(new Date(10000000L), r1.getDate("field7", new Date(10000001L)));
		assertEquals(new Date(10000001L), r1.getDate("unknown", new Date(10000001L)));		
	}

	/**
	 * Test toStringMeta().
	 */
	public void testToStringMeta()
	{
		Row r0 = new Row();

		Value values[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", true),                    // Boolean
				new Value("field5", new BigDecimal(123.0)),   // BigDecimal
				new Value("field6", (String)null),            // NULL value
				null                                          // null
		};

		Value values1[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field5", new BigDecimal(123.0)),   // BigDecimal
		};
		
		values1[0].setLength(10, 2);
		values1[1].setLength(4,  0);
		values1[2].setLength(5,  3);
		values1[3].setLength(6,  1);
		
		Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}

		Row r2 = new Row();
		for (int i=0; i < values1.length; i++ )
		{
			r2.addValue(values1[i]);
		}

		assertEquals("[]", r0.toStringMeta());
		assertEquals("[field1(String), field2(Integer), field3(Number), field4(Boolean), field5(BigNumber), field6(String), NULL]", r1.toStringMeta());
		assertEquals("[field1(String(10)), field2(Integer(4)), field3(Number(5,3)), field5(BigNumber(6,1))]", r2.toStringMeta());
	}
	
	/**
	 * Test toGetXML().
	 */
	public void testGetXML()
	{
		Value values[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", true),                    // Boolean				
				new Value("field5", new BigDecimal(123.0)),   // BigDecimal
				new Value("field6", (String)null)             // NULL value				
		};

		Row r1 = new Row();
		for (int i=0; i < values.length; i++ )
		{
			r1.addValue(values[i]);
		}
		
		assertEquals("<row><value><name>field1</name><type>String</type><text>KETTLE</text><length>-1</length><precision>-1</precision><isnull>N</isnull></value><value><name>field2</name><type>Integer</type><text> 123</text><length>-1</length><precision>0</precision><isnull>N</isnull></value><value><name>field3</name><type>Number</type><text>10.5</text><length>-1</length><precision>-1</precision><isnull>N</isnull></value><value><name>field4</name><type>Boolean</type><text>true</text><length>-1</length><precision>-1</precision><isnull>N</isnull></value><value><name>field5</name><type>BigNumber</type><text>123</text><length>-1</length><precision>-1</precision><isnull>N</isnull></value><value><name>field6</name><type>String</type><text/><length>-1</length><precision>-1</precision><isnull>Y</isnull></value></row>", r1.getXML());		
	}

	/**
	 * Test of setting and getting via indexes.
	 */
	public void testIndexes()
	{
	    Row r0 = new Row();
	    
	    assertTrue(r0.isEmpty());
	    
	    r0.addValue(new Value("dummy1", true)); 
	    r0.addValue(new Value("dummy2", true));
	    
	    r0.setValue(0, new Value("field1", true));
	    r0.setValue(1, new Value("field2", true));

	    assertEquals("field1", r0.getValue(0).getName());
	    assertEquals("field2", r0.getValue(1).getName());

	    r0.setValue(1, new Value("field3", true));
	    assertEquals("field3", r0.getValue(1).getName());
	}

	/**
	 * Test toCompare().
	 */
	public void testCompare()
	{
		Value values1[] = {
			    new Value("field1", "AAA"),
			    new Value("field2", "BBB"),
			    new Value("field3", "CCC"),			    
		};

		Value values2[] = {
			    new Value("field1", "aaa"),
			    new Value("field2", "bbb"),
			    new Value("field3", "DDD"),			    
		};		
		
		Row r1 = new Row();
		for (int i=0; i < values1.length; i++ )
		{
			r1.addValue(values1[i]);
		}

		Row r2 = new Row();
		for (int i=0; i < values2.length; i++ )
		{
			r2.addValue(values2[i]);
		}
		
		Row r3 = r1.Clone();

		// kind of comparison for specified nr of fields
		int [] fields1 = { 0 };
		boolean [] ascending1 =  { true }; 
		assertEquals(0, r1.compare(r3, fields1, ascending1));

		int [] fields2 = { 0, 1 };
		boolean [] ascending2 =  { true, false };
		boolean [] case2 =  { false, false };
		assertEquals(0, r1.compare(r1, fields2, ascending2, case2));

		int [] fields3 = { 0, 1, 2 };
		boolean [] ascending3 =  { true, false, false };
		boolean [] case3 =  { false, false, false };
		assertTrue(r1.compare(r2, fields3, ascending3, case3) < 0);
		
		// kind of comparison for specified nr of fields
		assertEquals(0, r1.compare(r2, 1, true));
		assertEquals(0, r1.compare(r2, 1, false));

		assertEquals(1, r1.compare(r2, 2, true));
		assertEquals(-1, r1.compare(r2, 2, false));
		assertEquals(-1, r2.compare(r1, 2, true));
		assertEquals(1, r2.compare(r1, 2, false));

		assertEquals(0, r1.compare(r1, 0, false));
		assertEquals(0, r1.compare(r1, 0, true));

		// Third way of comparing
		assertEquals(-1, r1.compare(r2));
		assertEquals(0, r1.compare(r1));

		// Do compareTo as well
		assertEquals(0,  r1.compareTo((Object)r1));
		assertEquals(0,  r1.compareTo((Object)r3));
		assertEquals(-1, r1.compareTo((Object)r2));
		assertEquals(1,  r2.compareTo((Object)r1));	
	}	 

	public void testGetFieldNamesAndTypes() 
	{
		Value values1[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", true),                    // Boolean
				new Value("field5", new BigDecimal(123.0)),   // BigDecimal
				new Value("field6", (String)null),            // NULL value
				new Value("field7", new Date(10000000L)),     // Date
		};

		Row r1 = new Row();
		for (int i=0; i < values1.length; i++ )
		{
			r1.addValue(values1[i]);
		}
		
		String result = "";
		String a1[] = r1.getFieldNamesAndTypes(20);
		assertEquals(7, a1.length);
		for ( int idx = 0; idx < a1.length; idx++ )
		{
			result += a1[idx];
		}
		assertEquals("field1                 (String)field2                 (Integer)field3                 (Number)field4                 (Boolean)field5                 (BigNumber)field6                 (String)field7                 (Date)", result);

		result = "";
		a1 = r1.getFieldNamesAndTypes(10);
		assertEquals(7, a1.length);
		for ( int idx = 0; idx < a1.length; idx++ )
		{
			result += a1[idx];
		}
		assertEquals("field1       (String)field2       (Integer)field3       (Number)field4       (Boolean)field5       (BigNumber)field6       (String)field7       (Date)", result);

		result = "";
		a1 = r1.getFieldNamesAndTypes(4);
		assertEquals(7, a1.length);
		for ( int idx = 0; idx < a1.length; idx++ )
		{
			result += a1[idx];
		}
		assertEquals("fiel   (String)fiel   (Integer)fiel   (Number)fiel   (Boolean)fiel   (BigNumber)fiel   (String)fiel   (Date)", result);		
	}

	/**
	 * Test the read/write methods (not yet finished).
	 */
	public void testReadWrite() throws KettleFileException 
	{
		Value values1[] = {
			    new Value("field1", "KETTLE"),                // String
				new Value("field2", 123L),                    // integer
				new Value("field3", 10.5D),                   // double
				new Value("field4", true),                    // Boolean
				new Value("field5", new BigDecimal(123.0)),   // BigDecimal
				new Value("field6", (String)null),            // NULL value
				new Value("field7", new Date(10000000L)),     // Date
		};

		Row r1 = new Row();
		for (int i=0; i < values1.length; i++ )
		{
			r1.addValue(values1[i]);
		}

		DataOutputStream store = new DataOutputStream(new ByteArrayOutputStream());
		r1.writeData(store);
		
		// finish reading
	}
}
