package tangible;

//----------------------------------------------------------------------------------------
//	Copyright © 2007 - 2021 Tangible Software Solutions, Inc.
//	This class can be used by anyone provided that the copyright notice remains intact.
//
//	This class is used to convert some of the C# TryParse methods to Java.
//----------------------------------------------------------------------------------------
public final class TryParseHelper
{
	public static boolean tryParseInt(String s, tangible.OutObject<Integer> result)
	{
		try
		{
			result.outArgValue = Integer.parseInt(s);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	public static boolean tryParseShort(String s, tangible.OutObject<Short> result)
	{
		try
		{
			result.outArgValue = Short.parseShort(s);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	public static boolean tryParseLong(String s, tangible.OutObject<Long> result)
	{
		try
		{
			result.outArgValue = Long.parseLong(s);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	public static boolean tryParseByte(String s, tangible.OutObject<Byte> result)
	{
		try
		{
			result.outArgValue = Byte.parseByte(s);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	public static boolean tryParseDouble(String s, tangible.OutObject<Double> result)
	{
		try
		{
			result.outArgValue = Double.parseDouble(s);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	public static boolean tryParseFloat(String s, tangible.OutObject<Float> result)
	{
		try
		{
			result.outArgValue = Float.parseFloat(s);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}

	public static boolean tryParseBoolean(String s, tangible.OutObject<Boolean> result)
	{
		try
		{
			result.outArgValue = Boolean.parseBoolean(s);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
}