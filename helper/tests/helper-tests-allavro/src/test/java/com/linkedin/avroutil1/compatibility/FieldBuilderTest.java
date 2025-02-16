/*
 * Copyright 2021 LinkedIn Corp.
 * Licensed under the BSD 2-Clause License (the "License").
 * See License in the project root for license information.
 */

package com.linkedin.avroutil1.compatibility;

import com.linkedin.avroutil1.testcommon.TestUtil;
import org.apache.avro.Schema;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FieldBuilderTest {

    @Test
    public void testFieldPropSupportOnClone() throws Exception {
        AvroVersion runtimeAvroVersion = AvroCompatibilityHelper.getRuntimeAvroVersion();
        String avsc = TestUtil.load("RecordWithFieldProps.avsc");
        Schema schema = Schema.parse(avsc);
        Schema.Field originalField = schema.getField("stringField");

        FieldBuilder builder = AvroCompatibilityHelper.cloneSchemaField(originalField);
        Schema.Field newField = builder.build();

        Assert.assertNotSame(newField, originalField);
        Assert.assertNull(AvroCompatibilityHelper.getFieldPropAsJsonString(newField, "noSuchProp"));
        Assert.assertEquals(AvroCompatibilityHelper.getFieldPropAsJsonString(newField, "stringProp"), "\"stringValue\"");
        if (runtimeAvroVersion.earlierThan(AvroVersion.AVRO_1_7)) {
            //sadly avro 1.4/5/6 do not preserve any other (non textual) props
            return;
        }
        String intPropAsJsonLiteral = AvroCompatibilityHelper.getFieldPropAsJsonString(newField, "intProp");
        if (runtimeAvroVersion.equals(AvroVersion.AVRO_1_7) && intPropAsJsonLiteral == null) {
            //we must be under avro < 1.7.3. avro 1.7 only supports non-text props at 1.7.3+
            return;
        }
        Assert.assertEquals(intPropAsJsonLiteral, "42");
        Assert.assertEquals(AvroCompatibilityHelper.getFieldPropAsJsonString(newField, "floatProp"), "42.42");
        Assert.assertEquals(AvroCompatibilityHelper.getFieldPropAsJsonString(newField, "nullProp"), "null");
        Assert.assertEquals(AvroCompatibilityHelper.getFieldPropAsJsonString(newField, "boolProp"), "true");
        Assert.assertEquals(AvroCompatibilityHelper.getFieldPropAsJsonString(newField, "objectProp"), "{\"a\":\"b\",\"c\":\"d\"}");
    }

    /**
     * tests that the values returned by {@link AvroCompatibilityHelper#getGenericDefaultValue(Schema.Field)}
     * and/or {@link AvroCompatibilityHelper#getSpecificDefaultValue(Schema.Field)} can be used with
     * {@link FieldBuilder}
     * @throws Exception if poop hits the fan
     */
    @Test
    public void testDefaultValuesInterop() throws Exception {
        Schema schema = under14.HasSimpleDefaults.SCHEMA$;

        setDefaultValues(schema.getField("nullField"));
        setDefaultValues(schema.getField("boolField"));
        setDefaultValues(schema.getField("strField"));
        setDefaultValues(schema.getField("unionWithNullDefault"));
        setDefaultValues(schema.getField("unionWithStringDefault"));

        schema = under14.HasComplexDefaults.SCHEMA$;

        setDefaultValues(schema.getField("fieldWithDefaultEnum"));
        setDefaultValues(schema.getField("fieldWithDefaultFixed"));
        setDefaultValues(schema.getField("fieldWithDefaultRecord"));
        setDefaultValues(schema.getField("fieldWithDefaultIntArray"));
        setDefaultValues(schema.getField("unionFieldWithDefaultRecord"));
    }

    private void setDefaultValues(Schema.Field field) {
        Assert.assertTrue(AvroCompatibilityHelper.fieldHasDefault(field));
        Schema fieldSchema = field.schema();
        Object specificDefault = AvroCompatibilityHelper.getSpecificDefaultValue(field);
        Object genericDefault = AvroCompatibilityHelper.getGenericDefaultValue(field);

        //set default value using specifics
        Schema.Field newField = AvroCompatibilityHelper.newField(null)
                .setSchema(fieldSchema)
                .setDefault(specificDefault)
                .setName("excitingField") //required
                .setOrder(Schema.Field.Order.ASCENDING) //required?
                .build();

        //set default value using generics
        Schema.Field moreNewField = AvroCompatibilityHelper.newField(null)
                .setSchema(fieldSchema)
                .setDefault(genericDefault)
                .setName("excitingField") //required
                .setOrder(Schema.Field.Order.ASCENDING) //required?
                .build();

        Assert.assertEquals(newField, moreNewField);
    }
}
