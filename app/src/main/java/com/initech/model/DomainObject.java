package com.initech.model;

import org.json.JSONObject;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author Kevin
 * 
 *         The main value of this object is the toString() method. Use this
 *         toString if you want to get all fields/values into a string of ANY
 *         object or bean. Useful for debugging with objects that have a LOT of
 *         fields and you are lazy to write your own toString()
 * 
 *         You have two options, either you can: 1) extend this ToString in your
 *         object and it will just inherit toString()
 * 
 */
public abstract class DomainObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6366830768700755947L;

	public DomainObject() {
	}

	/**
	 * only converts java primitive types and also String, java.sql.Timestamp
	 * and java.util.Date
	 * 
	 * All other types won't be supported and need to be obtained thru other
	 * means
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public JSONObject toJSON() {

		final JSONObject o = new JSONObject();

		final Class thisklass = this.getClass();

		final Method[] methods = thisklass.getDeclaredMethods();
		for (int j = 0; j < methods.length; j++) {
			final Method m = methods[j];
			m.setAccessible(true);
			if (m.getName().startsWith("get")) {

				if ((m.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
						&& (m.getModifiers() & Modifier.STATIC) != Modifier.STATIC) {

					try {

						final StringBuilder sb = new StringBuilder();
						sb.append(m.getName().substring(3));
						sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
						final Field f = thisklass.getDeclaredField(sb.toString());
						f.setAccessible(true);

						if (f.getType().getName().equals("java.lang.String")) {

							try {
								
								o.put(f.getName(), m.invoke(this));
								
							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("int")) {

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("boolean")) {

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("float")) {

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("double")) {

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals(
								"java.sql.Timestamp")) { // mm-dd-yyyy

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("short")) {

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("long")) {

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals(
								"java.util.Date")) { // mm-dd-yyyy

							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}
							
						} else {  //must be some object or array of objects 
							
							try {

								o.put(f.getName(), m.invoke(this));

							} catch (final Exception e) {
								//LOGGER.error("convertToJSON: " + f.getName(), e);
							}							

						}

					} catch (final IllegalArgumentException e) {
						//e.printStackTrace();
					} catch (final NoSuchFieldException e) {
						//e.printStackTrace();
					}
				}

			}
		}

		return o;
	}

	public Object copyFrom(final Object inObject) {
		return copyFrom(inObject, "id");
	}

	public Object copyFrom(final Object inObject, final String exceptionField) {
		return copyFrom(inObject, exceptionField, false);
	}

	/**
	 * 
	 * only converts java primitive types and also String, java.sql.Timestamp,
	 * and java.util.Date
	 * 
	 * All other types won't be supported and need to be obtained thru other
	 * means
	 * 
	 * @param inObject
	 *            - object to be copied from, can be any java bean or JSONObject
	 * @param exceptionField
	 *            - name of field to be ignored, typically "id"
	 * @param getSuperclass
	 *            - obtain fields of superclass
	 * 
	 * @return Object - must cast to concrete object
	 */
	@SuppressWarnings("unchecked")
	public Object copyFrom(final Object inObject, final String exceptionField,
						   final boolean getSuperclass) {

		final Class thisklass = getSuperclass ? this.getClass().getSuperclass() :
						this.getClass();

		final Method[] methods = thisklass.getDeclaredMethods();
		for (int j = 0; j < methods.length; j++) {
			final Method m = methods[j];
			m.setAccessible(true);
			if (m.getName().startsWith("get")) {

				if ((m.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
						&& (m.getModifiers() & Modifier.STATIC) != Modifier.STATIC) {

					try {

						final StringBuilder sb = new StringBuilder();
						sb.append(m.getName().substring(3));
						sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
						final Field f = thisklass.getDeclaredField(sb
								.toString());
						f.setAccessible(true);
						
						if (exceptionField != null && sb.toString().equals(exceptionField) || (sb.toString().equals("serialVersionUID"))) {
							continue;
						}

//						System.out.println("method name=" + m.getName() + "="
//								+ m.invoke(inObject) + " field=" + f.getName());
						if (f.getType().getName().equals("java.lang.String")) {

							try {
								if (inObject instanceof JSONObject) {

									final String string = ((JSONObject) inObject)
											.getString(f.getName());
									if (string.equals(JSONObject.NULL
											.toString())) {
										f.set(this, null);
									} else {
										f.set(this, string);
									}

								} else {
									f.set(this, m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("int")) {

							try {

								if (inObject instanceof JSONObject) {
									f.setInt(this, ((JSONObject) inObject)
											.getInt(f.getName()));
								} else {
									f.setInt(this, (Integer)m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("boolean")) {

							try {

								if (inObject instanceof JSONObject) {
									f.setBoolean(this, ((JSONObject) inObject)
											.getBoolean(f.getName()));
								} else {
									f.setBoolean(this, (Boolean)m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("float")) {

							try {

								if (inObject instanceof JSONObject) {
									f.setFloat(this,
											(float) ((JSONObject) inObject)
													.getDouble(f.getName()));
								} else {
									f.setFloat(this, (Float)m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("double")) {

							try {

								if (inObject instanceof JSONObject) {
									f.setDouble(this, ((JSONObject) inObject)
											.getDouble(f.getName()));
								} else {
									f.setDouble(this, (Double)m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals(
								"java.sql.Timestamp")) { // mm-dd-yyyy

							try {

								if (inObject instanceof JSONObject) {

									final String string = ((JSONObject) inObject)
											.getString(f.getName());
									if (string.equals(JSONObject.NULL
											.toString())) {
										f.set(this, null);
									} else {
										f.set(this,convertStringToTimestampHourMin(string));
									}

								} else {
									f.set(this, m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("short")) {

							try {

								if (inObject instanceof JSONObject) {
									f.setShort(this,
											(short) ((JSONObject) inObject)
													.getInt(f.getName()));
								} else {
									f.setShort(this, (Short)m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals("long")) {

							try {
								if (inObject instanceof JSONObject) {
									f.setLong(this, ((JSONObject) inObject)
											.getLong(f.getName()));
								} else {
									f.setLong(this, (Long)m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}

						} else if (f.getType().getName().equals(
								"java.util.Date")) { // mm-dd-yyyy

							try {

								if (inObject instanceof JSONObject) {

									final String string = ((JSONObject) inObject)
											.getString(f.getName());
									if (string.equals(JSONObject.NULL
											.toString())) {
										f.set(this, null);
									} else {
										convertToDate(((JSONObject) inObject)
												.getString(f.getName()));
									}

								} else {
									f.set(this, m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}
							
						} else {  //must be some object or array of objects 
							
							try {

								if (inObject instanceof JSONObject) {

									//for now, don't support the json side

								} else {
									f.set(this, m.invoke(inObject));
								}

							} catch (final Exception e) {
								//LOGGER.error("copyFrom: " + f.getName(), e);
							}							
							
						}

					} catch (final IllegalArgumentException e) {
						// e.printStackTrace();
					} catch (final NoSuchFieldException e) {
						// e.printStackTrace();
					}
				}

			}
		}

		return this;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(final boolean getSuperclass) {
		final StringBuilder buff = new StringBuilder();

		final Field[] fields = getSuperclass ? this.getClass().getSuperclass()
				.getDeclaredFields() : this.getClass().getDeclaredFields();

		for (int i = 0; i < fields.length; i++) {
			try {
				// only need ReflectionHelper if < JDK1.5
				final Field rf = getField(this.getClass(), fields[i].getName());

				buff.append('[').append(rf.getName()).append(" => ").append(
						rf.get(this)).append(']');

			} catch (final Exception e) {
			}
		}
		return buff.toString();
	}

	/**
	 * True if the JDK implements Java 2 or above. If this is
	 * <code>true</true>, then non-public classes and members can be accessed.
	 */
	public static boolean isJava2;

	static {
		try {
			Class.forName("java.lang.reflect.AccessibleObject");
			isJava2 = true;
		} catch (final ClassNotFoundException ex) {
			isJava2 = false;
		}
	}

	/** Override Java-Access-control, if possible. */
	public static void enableAccess(final Object obj) {
		setAccessible(obj, true);
	}

	// going thru reflection so this can be compiled using JDK1.1.
	static Method setAccessibleMethod = null;

	@SuppressWarnings("unchecked")
	static void setAccessible(final Object obj, final boolean accessible) {
		try {
			if (setAccessibleMethod == null) {
				final Class aclass = Class
						.forName("java.lang.reflect.AccessibleObject");
				setAccessibleMethod = aclass.getMethod("setAccessible",
						new Class[] { Boolean.TYPE });
			}
			setAccessibleMethod.invoke(obj, new Object[] { Boolean.TRUE });
		} catch (Throwable e) {
			//System.out.println("Error trying to set accessibility for " + obj);
		}
	}

	@SuppressWarnings("unchecked")
	public static java.lang.reflect.Field getField(final Class clas,
			final String fname) {

		java.lang.reflect.Field field;

		try {
			field = clas.getField(fname);
		} catch (NoSuchFieldException ex) {
			if (isJava2) {
				// try harder, using all declared fields.
				java.lang.reflect.Field[] localFields = clas
						.getDeclaredFields();

				for (int i = 0; i != localFields.length; i++) {
					if (fname.equals(localFields[i].getName())) {
						field = localFields[i];
						enableAccess(field);
						return field;
					}
				}
				// if not found, go up to superclass.
				final Class superClass = clas.getSuperclass();
				if (superClass == null)
					field = null;
				else
					return getField(superClass, fname);
			} else
				field = null;
		} catch (final Exception ex) {
			field = null;
		}
		return field;
	}

	private static Timestamp convertStringToTimestampHourMin(final String s) {

		try {
			final Date d = DATE_AND_HOUR_FORMAT.parse(s);
			return new Timestamp(d.getTime());

		} catch (final Exception e) {
		}
		return null;
	}

	private static Date convertToDate(final String s) {

		try {
			return DATE_FORMAT.parse(s);
		} catch (final Exception e) {
		}
		return null;
	}

	/*
	 * works with mysql
	 */
	private static final SimpleDateFormat DATE_AND_HOUR_FORMAT = new SimpleDateFormat(
			"yyyy-MM-dd hh:mm:ss");

	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"MM-dd-yyyy");

	//private final static LOGGER = getLOGGER(DomainObject.class);
}
