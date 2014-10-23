/**
 * 
 */
package org.korsakow.ide.code.k5;

import java.util.ArrayList;
import java.util.List;

import org.korsakow.domain.Keyword;
import org.korsakow.ide.code.RuleParserException;
import org.korsakow.ide.rules.RuleType;
import org.korsakow.ide.ui.model.RuleModel;

/**
 * K5 source code parser. With helper methods to generate K5 Rules.
 * @deprecated
 * @author d
 *
 */
@Deprecated
public class K5RuleParser2
{
	List<K5Lexeme> k5Tokens;
	public boolean isValidKeyword(String keyword)
	{
		if (keyword == null)
			return false;
		return true;
		// validation is a pain. it implies:
		//	1) complex conversion of k3 projects
		//  2) complex handling of the resource-name-as-keyword feature
		//  3) complexities related to the use of filenames as resource names / keywords
		//return keyword != null && keyword.matches("^(?:\\p{L}|[0-9]|[^+-])+"); // p{L] = any unicode letter
	}
	public void validateKeyword(String keyword) throws RuleParserException
	{
		if (!isValidKeyword(keyword))
			throw new RuleParserException("invalid keyword:" + keyword);
	}
	public List<K5Lexeme> tokenize(String code) throws RuleParserException
	{
		List<K5Lexeme> tokens = new ArrayList<>();
		String[] parts = code.split(String.format("[%s\\s]", K5Symbol.DEFAULT_STATEMENT_SEPARATOR_STRING));
		for (String part : parts) {
			part = part.trim();
			if (part.length() == 0)
				continue;
			
			char op = part.charAt(part.length()-1);
			String keyword = part.substring(0, part.length()-1);
			K5Lexeme lexeme;
			if (part.equals(K5Symbol.CLEAR_PREVIOUS_LINKS)) {
				lexeme = new K5Lexeme(K5OpType.CLEAR_PREVIOUS_LINKS, part);
			} else
			if (part.equals(K5Symbol.KEEP_PREVIOUS_LINKS)) {
				lexeme = new K5Lexeme(K5OpType.KEEP_PREVIOUS_LINKS, part);
			} else {
				switch (op)
				{
				case K5Symbol.EXCLUSION_KEYWORD:
					if (K5Symbol.RANDOM_KEYWORD.equals(keyword))
						throw new RuleParserException("cannot use '"+keyword+"' in this context");
					validateKeyword(keyword);
					lexeme = new K5Lexeme(K5OpType.KEYWORD_EXCLUSION, op, keyword);
					break;
				case K5Symbol.REQUIRED_KEYWORD:
					if (K5Symbol.RANDOM_KEYWORD.equals(keyword))
						throw new RuleParserException("cannot use '"+keyword+"' in this context");
					validateKeyword(keyword);
					lexeme = new K5Lexeme(K5OpType.KEYWORD_REQUIRED, op, keyword);
					break;
				default:
					if (isValidKeyword(keyword + op)) {
						lexeme = new K5Lexeme(K5OpType.KEYWORD_LOOKUP, keyword + op);
					} else
						throw new RuleParserException("invalid op: " + op);
				}
			}
			tokens.add(lexeme);
		}
		return tokens;
	}
	public List<RuleModel> createRules(List<K5Lexeme> lexemes) throws RuleParserException
	{
		List<RuleModel> rules = new ArrayList<RuleModel>();
		List<String> outboundKeywords = new ArrayList<String>();
		List<String> requireKeywords = new ArrayList<String>();
		List<String> excludeKeywords = new ArrayList<String>();
		boolean clearScores = false;
		boolean setendfilm = false;
		for (K5Lexeme lexeme : lexemes)
		{
			switch(lexeme.getOpType())
			{
			case KEYWORD_LOOKUP:
				outboundKeywords.add(lexeme.getToken());
				break;
			case KEYWORD_REQUIRED:
				requireKeywords.add(lexeme.getToken());
				break;
			case KEYWORD_EXCLUSION:
				excludeKeywords.add(lexeme.getToken());
				break;
			case CLEAR_PREVIOUS_LINKS:
				clearScores = true;
				break;
			case KEEP_PREVIOUS_LINKS:
				clearScores = false;
				break;
			default:
				throw new RuleParserException("k5 rule not yet parsable: " + lexeme.getOpType());
			}
		}
		// the order in which we generate most of the rules is important!
		// generally: Lookup, Require, Exclude
		
		if (clearScores) {
			RuleModel rule = new RuleModel(RuleType.ClearScores);
			rules.add(rule);
		}
		
		if (setendfilm) { // this rule is order-independant
			RuleModel rule = new RuleModel(RuleType.SetEndfilm);
			rules.add(rule);
		}
		
		{ // scope local vars for clarity
			List<String> regularKeywords = new ArrayList<String>();
			List<String> randomKeywords = new ArrayList<String>();
			List<String> endfilmKeywords = new ArrayList<String>();
			for (String keyword : outboundKeywords) {
				if (K5Symbol.RANDOM_KEYWORD.equals(keyword)) {
					randomKeywords.add(keyword);
				} else if (K5Symbol.ENDFILM_KEYWORD.equals(keyword)) {
					endfilmKeywords.add(keyword);
				} else {
					regularKeywords.add(keyword);
				}
			}
			// the order of random/endfilm vs regular is not important
			for (String random : randomKeywords) {
				RuleModel rule = new RuleModel(RuleType.RandomLookup);
				rules.add(rule);
			}
			for (String endfilm : endfilmKeywords) {
				RuleModel rule = new RuleModel(RuleType.EndfilmLookup);
				rules.add(rule);
			}
			if (!regularKeywords.isEmpty()) {
				RuleModel rule = new RuleModel(RuleType.KeywordLookup);
				rule.setProperty("keywords", Keyword.fromStrings(regularKeywords));
				rules.add(rule);
			}
		}
		
		{ // scope local vars for clarity
			// requirekeywords must be after all other keyword lookup rules except exclusion
			List<String> regularRequires = new ArrayList<String>();
			List<String> endfilmRequires = new ArrayList<String>();
			for (String keyword : requireKeywords) {
				if (K5Symbol.ENDFILM_KEYWORD.equals(keyword)) {
					endfilmRequires.add(keyword);
				} else {
					regularRequires.add(keyword);
				}
			}
			// the order of endfilm vs regular is not important
			for (String endfilm : endfilmRequires) {
				RuleModel rule = new RuleModel(RuleType.RequireEndfilm);
				rules.add(rule);
			}
			if (!regularRequires.isEmpty()) {
				RuleModel rule = new RuleModel(RuleType.RequireKeywords);
				rule.setProperty("keywords", Keyword.fromStrings(regularRequires));
				rules.add(rule);
			}
		}
		
		{ // scope local vars for clarity
			// exclude keywords must be after all other keyword lookup rules
			List<String> regularExcludes = new ArrayList<String>();
			List<String> endfilmExcludes = new ArrayList<String>();
			for (String keyword : excludeKeywords) {
				if (K5Symbol.ENDFILM_KEYWORD.equals(keyword)) {
					endfilmExcludes.add(keyword);
				} else {
					regularExcludes.add(keyword);
				}
			}
			// the order of endfilm vs regular is not important
			for (String endfilm : endfilmExcludes) {
				RuleModel rule = new RuleModel(RuleType.ExcludeEndfilm);
				rules.add(rule);
			}
			if (!regularExcludes.isEmpty()) {
				RuleModel rule = new RuleModel(RuleType.ExcludeKeywords);
				rule.setProperty("keywords", Keyword.fromStrings(regularExcludes));
				rules.add(rule);
			}
		}
		
		return rules;
	}
	/**
	 * K5-code and K5 rules are many-to-many. That is any one K5 code might correspond to many K5 rules
	 * and vice versa.
	 * 
	 * @param code
	 * @param triggerTime if null no time is set
	 * @return
	 * @throws RuleParserException
	 */
	public List<RuleModel> parse(String code, Long triggerTime) throws RuleParserException
	{
		k5Tokens = tokenize(code);
		List<RuleModel> rules = createRules(k5Tokens);
		if (triggerTime != null) {
			for (RuleModel rule : rules)
				rule.setProperty("triggerTime", triggerTime);
		}
		return rules;
	}
	public List<RuleModel> parse(String code) throws RuleParserException
	{
		return parse(code, null);
	}
}