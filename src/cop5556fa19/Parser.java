/**
 * Developed  for the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Fall 2019.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Fall 2019 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2019
 */

package cop5556fa19;
	
import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import cop5556fa19.AST.Block;
import cop5556fa19.AST.Chunk;
import cop5556fa19.AST.Exp;
import cop5556fa19.AST.ExpBinary;
import cop5556fa19.AST.ExpFalse;
import cop5556fa19.AST.ExpFunction;
import cop5556fa19.AST.ExpFunctionCall;
import cop5556fa19.AST.ExpInt;
import cop5556fa19.AST.ExpList;
import cop5556fa19.AST.ExpName;
import cop5556fa19.AST.ExpNil;
import cop5556fa19.AST.ExpString;
import cop5556fa19.AST.ExpTable;
import cop5556fa19.AST.ExpTableLookup;
import cop5556fa19.AST.ExpTrue;
import cop5556fa19.AST.ExpUnary;
import cop5556fa19.AST.ExpVarArgs;
import cop5556fa19.AST.Field;
import cop5556fa19.AST.FieldExpKey;
import cop5556fa19.AST.FieldImplicitKey;
import cop5556fa19.AST.FieldNameKey;
import cop5556fa19.AST.FuncBody;
import cop5556fa19.AST.FuncName;
import cop5556fa19.AST.Name;
import cop5556fa19.AST.ParList;
import cop5556fa19.AST.RetStat;
import cop5556fa19.AST.Stat;
import cop5556fa19.AST.StatAssign;
import cop5556fa19.AST.StatBreak;
import cop5556fa19.AST.StatDo;
import cop5556fa19.AST.StatFor;
import cop5556fa19.AST.StatGoto;
import cop5556fa19.AST.StatIf;
import cop5556fa19.AST.StatLabel;
import cop5556fa19.AST.StatLocalAssign;
import cop5556fa19.AST.StatLocalFunc;
import cop5556fa19.AST.StatForEach;
import cop5556fa19.AST.StatFunction;
import cop5556fa19.AST.StatRepeat;
import cop5556fa19.AST.StatWhile;
import cop5556fa19.Token.Kind;
import static cop5556fa19.Token.Kind.*;

public class Parser{
	
	@SuppressWarnings("serial")
	public
	class SyntaxException extends Exception {
		Token t;
		
		public SyntaxException(Token t, String message) {
			super(t.line + ":" + t.pos + " " + message);
		}
	}
	
	final Scanner scanner;
	public Token t;  //invariant:  this is the next token
	
	public Parser(Scanner s) throws Exception {
		this.scanner = s;
		t = scanner.getNext(); //establish invariant
	}
	
	int precedence(Token op) {
		
		if(op.kind == KW_or) {
			return 1;
		}
		else if(op.kind == KW_and) {
			return 2;
		}
		else if(op.kind == REL_LT || op.kind == REL_GT || op.kind == REL_LE ||
				op.kind == REL_GE || op.kind == REL_NOTEQ || op.kind == REL_EQEQ) {
			return 3;
		}
		else if(op.kind == BIT_OR) {
			return 4;
		}
		else if(op.kind == BIT_XOR) {
			return 5;
		}
		else if(op.kind == BIT_AMP) {
			return 6;
		}
		else if(op.kind == BIT_SHIFTL || op.kind == BIT_SHIFTR) {
			return 7;
		}
		else if(op.kind == DOTDOT) {
			return 8;
		}
		else if(op.kind == OP_PLUS || op.kind == OP_MINUS) {
			return 9;
		}
		else if(op.kind == OP_TIMES || op.kind == OP_DIV || op.kind == OP_DIVDIV || 
				op.kind == OP_MOD) {
			return 10;
		}
		else if(op.kind == OP_POW) {
			return 12;
		}
		else if(op.kind == DOTDOTDOT) {
			return 0;
		}
		else {
			return 0;
		}
		
	}
	
	public Exp exp() throws Exception {
		Stack<Exp> expStack = new Stack<Exp>();
		Stack<Token> opStack = new Stack<Token>();
		Token first = t;
		
		Exp e0 = null;
		if(isKind(EOF)) {
			return null;
		}
		if(isKind(OP_MINUS) || isKind(OP_HASH) || isKind(KW_not) || isKind(BIT_XOR)) {
			Token op = consume();
			Exp e1 = exp();
			if(e1 == null) {
				
				error(t, " is not valid for unary");
			}
			else {
			
				opStack.push(new Token(DOTDOTDOT, "...", 0, 0));
				opStack.push(op);
				expStack.push(e1);
				
			}
		
		}
		else {
		
			e0 = getExp();

			if(e0 == null) {
				error(t, " is not valid for unary");
			}
			else {
				expStack.push(e0);
				opStack.push(new Token(DOTDOTDOT, "...", 0, 0));
			}
		}
			
			while ( isKind(OP_PLUS) 	|| isKind(OP_MINUS)   || isKind(OP_TIMES) 	|| 
					isKind(OP_DIV)  	|| isKind(OP_DIVDIV)  || isKind(OP_MOD)  	|| 
					isKind(BIT_AMP) 	|| isKind(BIT_XOR) 	  || isKind(BIT_OR)  	|| 
					isKind(BIT_SHIFTR)  || isKind(BIT_SHIFTL) || isKind(REL_EQEQ)   ||
					isKind(REL_NOTEQ)   || isKind(REL_LE)     || isKind(REL_GE)     || 
					isKind(REL_LT) 		|| isKind(REL_GT)     || isKind(KW_and)     || 
					isKind(KW_or)		|| isKind(DOTDOT)     || isKind(OP_POW)) {
				
				Token op = consume();
				if(opStack.size() ==2 && expStack.size() == 1 && op.kind != OP_POW) {
					
					Exp e6 = expStack.pop();
					Token op2 = opStack.pop();
					expStack.push(new ExpUnary(first, op2.kind, e6));
				}
				Exp e1 = getExp();
				if(e1 == null) {
					error(t, " is not valid");
				}
				if(precedence(opStack.peek()) < precedence(op) || 
				   (opStack.peek().kind == OP_POW && op.kind == OP_POW)      ||
				   (opStack.peek().kind == DOTDOT && op.kind == DOTDOT) ){
					
					opStack.push(op);
					expStack.push(e1);
				}
				else {
					while (!(precedence(opStack.peek()) < precedence(op)) && expStack.size()>1) {
						
						Exp e5 = expStack.pop();
						Exp e4 = expStack.pop();
						Token op1 = opStack.pop();
						Exp e6 = new ExpBinary(first, e4, op1, e5);
						expStack.push(e6);
						
						
					}
					opStack.push(op);
					expStack.push(e1);
				}
				
				
			
			}
			
			if (expStack.size() > 1) {
				while (expStack.size() > 1) {
					Exp e5 = expStack.pop();
					Exp e4 = expStack.pop();
					Token op1 = opStack.pop();
					Exp e6 = new ExpBinary(first, e4, op1, e5);
					expStack.push(e6);
				}
				
			}
			
			if(opStack.size() ==2 && expStack.size() == 1) {
				Exp e6 = expStack.pop();
				Token op2 = opStack.pop();
				expStack.push(new ExpUnary(first, op2.kind, e6));
			}
			
			return expStack.pop();
		
			
	}

	private Exp getExp() throws Exception{
		// TODO Auto-generated method stub
		//throw new UnsupportedOperationException("andExp");  //I find this is a more useful placeholder than returning null.
		Exp e0 = null;
		
		if(isKind(KW_nil)) {
			e0 = new ExpNil(t);
			consume();
		}
		else if(isKind(KW_false)) {
			e0 = new ExpFalse(t);
			consume();
		}
		else if(isKind(KW_true)) {
			e0 = new ExpTrue(t);
			consume();
		}
		
		else if(isKind(INTLIT)) {
			e0 = new ExpInt(t);
			consume();
		}
		
		else if(isKind(STRINGLIT)) {
			e0 = new ExpString(t);
			consume();
		}
		
		else if(isKind(DOTDOTDOT)) {
			e0 = new ExpVarArgs(t);
			consume();
		}
		
		//FUNCTION
		else if(isKind(KW_function)) {
			e0 = Function();
		}
		
		//PRE FIX EXP
		else if (isKind(NAME) || isKind(LPAREN)) {
			e0 = PreFixExp();
					
		}
		
		//TABLE
		else if(isKind(LCURLY)) {
			e0 = Table();
		}
		
		return e0;
	}

	private Exp PreFixExp() throws Exception{
		Exp e0 = null;
		
		if (isKind(NAME)) {	
			e0 = new ExpName(t);
			consume();
			e0 = prefixExpTail(e0);	
		}
		
		else if(isKind(LPAREN)) {
			consume();		
			while(!isKind(RPAREN)) {
				if(isKind(EOF)) {
					error(t, " missing closing bracket");
				}
				e0 = exp();
			}
			consume();
			e0 = prefixExpTail(e0);
		}
		else {
			error(t, " wrong checking of prefix exp");
		}
		
		return e0;
	}

	private Exp Function() throws Exception {
		Exp e0 = null;
		Token first = t;
		if(isKind(KW_function)) {
			consume();
		}
		else {
			error(t, " wrong checking of function");
		}
		
		e0 = new ExpFunction(first, FuncBody());
		
		
		return e0;
		
	}

	private FuncBody FuncBody() throws Exception{
		FuncBody e0 = null;
		Token first = t;
		
		ParList p = null;
		List<Name> nameList = new ArrayList<Name>();
		if(isKind(LPAREN)) {
			consume();
			
			
			if(isKind(NAME)) {
				nameList.add(new Name(first, consume().getName()));
				while(!isKind(RPAREN)) {
					if(isKind(EOF)) {
						error(t, " missing closing bracket");
					}
					if(isKind(COMMA)) {
						consume();
						if(isKind(DOTDOTDOT)) {
							consume();
							if(!isKind(RPAREN)) {
								error(t, " missing closing bracket");
							}
							else {
								consume();
								Block b = block();
								if(isKind(KW_end)) {
									p = new ParList(first, nameList, true);
									e0 = new FuncBody(first, p, b);
									consume();
									return e0;
									
								}
								else {
									error(t, " missing end");
								}
								
							}
						}
					}
					else {
						error(t, " missing comma");
					}
					
					if(isKind(NAME)) {
						nameList.add(new Name(first, consume().getName()));
					}
					else {
						error(t, " missing name");
					}
				}
				consume();
				p = new ParList(first, nameList, false);
			}
			
			else if(isKind(DOTDOTDOT)) {
				consume();
				if(!isKind(RPAREN)) {
					error(t, " missing closing bracket");
				}
				else {
					consume();
					p = new ParList(first, nameList, true);
				}
			}
			else {
				if(!isKind(RPAREN)) {
					
					error(t, " missing closing bracket");
				}
				else {
					consume();
					p = new ParList(first, nameList, false);
				}
			}
			
		}
		else {
			error(t, " missing starting bracket");
		}
		Block b = block();
		if(isKind(KW_end)) {
			e0 =  new FuncBody(first, p, b);
			consume();
			
		}
		else {
			error(t, " missing end");
		}
		
		return e0;
	}
	
	private FuncName FuncName() throws Exception{
		FuncName f0 = null;
		List<ExpName> names = new ArrayList<ExpName>();
		if(isKind(NAME)) {
			names.add(new ExpName(t));
			consume();
			while(isKind(DOT)) {
				consume();
				if(isKind(NAME)) {
					names.add(new ExpName(t));
					consume();	
				}
				else {
					error(t, " missing identifier");
				}
			}
			if(isKind(COLON)) {
				consume();
				ExpName afterColon = null;
				if(isKind(NAME)) {
					afterColon = new ExpName(t);
					consume();
					f0 = new FuncName(t, names, afterColon);
				}
				else {
					error(t, " missing identifier");
				}
			}
			else {
				f0 = new FuncName(t, names, null);
			}
		}
		else {
			error(t, " missing identifier");
		}
		
		
		return f0;
	}
	
	private Exp Table() throws Exception {
		Exp e0 = null;
		Token first = t;
		if(isKind(LCURLY)) {
			consume();
		}
		else {
			error(t, " wrong checking of table");
		}
		List<Field> fields = new ArrayList<Field>();
		if(isKind(RCURLY)) {
			e0 = new ExpTable(first, fields);
			consume();
			return e0;
		}
		else {
			fields.add(getField());
			
			while(isKind(COMMA) || isKind(SEMI)) {
				consume();
				if(isKind(RCURLY)) {
					e0 = new ExpTable(first, fields);
					consume();
					return e0;
				}
				else {
					fields.add(getField());
					
				}
			}
			
			if(isKind(RCURLY)) {
				e0 = new ExpTable(first, fields);
				consume();
				return e0;
			}
			else {

				error(t, " missing closing bracket");
			}

		}
		return e0;
	}

	private Field getField() throws Exception{
		
		Token first = t;
		Field currentField = null;
		Token name = t;
		if(isKind(LSQUARE)) {
			consume();
			Exp key = exp();
			if(key != null) {
				if(isKind(RSQUARE)) {
					consume();
					if(isKind(ASSIGN)) {
						consume();
						Exp value = exp();
						if(value != null) {
							
							currentField = new FieldExpKey(first, key, value);
						}
						else {
							error(t, " invalid value after assign");
						}
					}
					else {
					
						error(t, " missing assign");
					}
				}
				else {
					error(t, " missing closing square bracket");
				}
			}
			else {
				error(t, " invalid key inside square brackets");
			}
		}
		else if(isKind(NAME)) {
			Token temp = t;
			consume();
			if(isKind(ASSIGN)) {
				
				consume();
				Exp key = exp();
				if(key != null) {
					
					currentField = new FieldNameKey(first, new Name(first, name.getName()), key);
				}
				else {
					error(t, " invalid value after assign");
				}
			}
			else {
				
				Exp value = new ExpName(temp);
				if (isKind(LPAREN)) {
		
						
					List<Exp> e0 = args();
						
					Exp e1 = new ExpFunctionCall(t, value, e0);
					Exp e2 =  prefixExpTail(e1);
					currentField = new FieldImplicitKey(first, e2);
						
					
				}
				else {
					currentField = new FieldImplicitKey(first, value);
				}
				
				
			}
			
		}
		else {
			
			Exp value = exp();
			
			if(value != null) {
				
				currentField = new FieldImplicitKey(first, value);
			}
			else {
				error(t, " invalid expression");
			}
		}
		return currentField;
	}
	
	private Exp prefixExpTail(Exp input) throws Exception {
		
		if(isKind(LSQUARE)) {
			consume();
			Exp e0 = exp();
			if (isKind(RSQUARE)) {
				consume();
				Exp e1 = new ExpTableLookup(t, input, e0);
				Exp e2 = prefixExpTail(e1);
				return e2;
			}
			else {
				error(t, " missing closing bracket");
			}
		}
		else if(isKind(DOT)) {
			consume();
			if (isKind(NAME)) {
				Exp e0 = new ExpString(t);
				consume();
				Exp e1 = new ExpTableLookup(t, input, e0);
				Exp e2 = prefixExpTail(e1);
				return e2;
			}
		}
		else if(isKind(LPAREN) | isKind(STRINGLIT) | isKind(LCURLY)) {
			
			List<Exp> e0 = args();
			
			Exp e1 = new ExpFunctionCall(t, input, e0);
			Exp e2 =  prefixExpTail(e1);
			return e2;
			
		}
		else if(isKind(COLON)) {
			consume();
			if (isKind(NAME)) {
				Exp e0 = new ExpString(t);
				consume();
				List<Exp> e1 = args();
				Exp e2 = new ExpTableLookup(t, input, e0);
				e1.add(0, input);
				Exp e3 = new ExpFunctionCall(t, e2, e1);
				Exp e4 = prefixExpTail(e3);
				return e4;
			}
			else {
				error(t, " missing identifier");
			}
		}
		
		return input;
		
	}
	
	private List<Exp> args() throws Exception {
		List<Exp> e0 = new ArrayList<Exp>();
		
		if(isKind(LPAREN)) {
			
			consume();	
			
			e0.addAll(expListArgs2());
			
			if(isKind(RPAREN)) {
				
				consume();
			}
			else {
				
				error(t, " missing closing bracket");
			}
			
		}
		else if(isKind(STRINGLIT)) {
			e0.add(new ExpString(t));
			consume();
		}
		else if(isKind(LCURLY)) {
			e0.add(Table());
		}
		else {
			error(t, " wrong args");
		}
		return e0;
	}

	private List<Exp> expListArgs2() throws Exception {
		List<Exp> e0 = new ArrayList<Exp>();
		Exp e2 = null;
		if(!isKind(RPAREN)) {
			e2 = exp();
		}
		
		if (e2 != null) {
			e0.add(e2);
		}
		else {
			
			return e0;
		}
		
		while(isKind(COMMA)) {
			consume();
			e2 = exp();
			if (e2 == null) {
				error(t, " no expression found");
			}
			else {
				e0.add(e2);
			}
		}
		
		
		return e0;
		
	}
	
	private List<Exp> expListArgs() throws Exception {

		List<Exp> e0 = new ArrayList<Exp>();
		Exp e2 = exp();
		if (e2 == null) {
			error(t, " no expression found");
		}
		else {
			e0.add(e2);
		}
		while(isKind(COMMA)) {
			consume();
			e2 = exp();
			if (e2 == null) {
				error(t, " no expression found");
			}
			else {
				e0.add(e2);
			}
		}
		
		
		return e0;
	}
	
	private Exp Var() throws Exception{
		Exp e0 = null;
		
		if (isKind(NAME)) {	
			e0 = new ExpName(t);
			consume();
			e0 = VarTail(e0);	
		}
		
		else if(isKind(LPAREN)) {
			consume();		
			while(!isKind(RPAREN)) {
				if(isKind(EOF)) {
					error(t, " missing closing bracket");
				}
				e0 = exp();
			}
			consume();
			Exp e1 = VarTail(e0);
			if(e1 == e0) {
				
				error(t, " illegal var end");
			}
			e0 = e1;
		}
		else {
			error(t, " wrong checking of prefix exp");
		}
		
		return e0;
	}

	private Exp VarTail(Exp input) throws Exception {
		
		if(isKind(LSQUARE)) {
			consume();
			Exp e0 = exp();
			if (isKind(RSQUARE)) {
				consume();
				Exp e1 = new ExpTableLookup(t, input, e0);
				Exp e2 = prefixExpTail(e1);
				return e2;
			}
			else {
				error(t, " missing closing bracket");
			}
		}
		else if(isKind(DOT)) {
			consume();
			if (isKind(NAME)) {
				Exp e0 = new ExpString(t);
				consume();
				Exp e1 = new ExpTableLookup(t, input, e0);
				Exp e2 = prefixExpTail(e1);
				return e2;
			}
		}
		else if(isKind(LPAREN) | isKind(STRINGLIT) | isKind(LCURLY)) {
			List<Exp> e0 = args();
			Exp e1 = new ExpFunctionCall(t, input, e0);
			Exp e2 =  prefixExpTail(e1);
			if(e1 == e2) {
				error(t, " illegal var end");
			}
			return e2;
			
		}
		else if(isKind(COLON)) {
			consume();
			if (isKind(NAME)) {
				Exp e0 = new ExpString(t);
				consume();
				List<Exp> e1 = args();
				Exp e2 = new ExpTableLookup(t, input, e0);
				e1.add(0, input);
				Exp e3 = new ExpFunctionCall(t, e2, e1);
				Exp e4 = prefixExpTail(e3);
				if(e3 == e4) {
					error(t, " illegal var end");
				}
				return e4;
			}
			else {
				error(t, " missing identifier");
			}
		}
		
		return input;
		
	}
	
	private boolean flagSEMI = false;
	
	private Stat Statement() throws Exception {
		Stat s0 = null;
		flagSEMI = false;
		
		if (isKind(SEMI)) {
			consume();
			flagSEMI = true;
		}
		
		else if(isKind(NAME) || isKind(LPAREN)) {
			List<Exp> l0 = new ArrayList<Exp>();
			List<Exp> l1 = new ArrayList<Exp>();
			l0.add(Var());
			while(isKind(COMMA)) {
				consume();
				l0.add(Var());
			}
			if(isKind(ASSIGN)) {
				consume();
				l1.addAll(expListArgs());
				s0 = new StatAssign(t, l0, l1);
			}
			else {
				error(t, " missing assign");
			}
		}
		
		else if(isKind(COLONCOLON)) {
			consume();
			if(isKind(NAME)) {
				Name label = new Name(t, t.text);
				consume();
				if(isKind(COLONCOLON)) {
					consume();
					s0 = new StatLabel(t, label);
				}
				else {
					error(t, " missing ending colon colon");
				}
			}
			else {
				error(t, " missing identifier");
			}
			
		}
		
		else if(isKind(KW_break)) {
			consume();
			s0 = new StatBreak(t);
		}
		
		else if(isKind(KW_goto)) {
			consume();
			if(isKind(NAME)) {
				Name name = new Name(t, t.text);
				consume();
				s0 = new StatGoto(t, name);
			}
			else {
				error(t, " missing identifier");
			}
		}
		
		else if(isKind(KW_do)) {
			consume();
			Block block = block();
			if(isKind(KW_end)) {
				consume();
				s0 = new StatDo(t, block); 
			}
			else {
				error(t, " missing end");
			}
		}
		
		else if(isKind(KW_while)) {
			consume();
			Exp e0 = exp();
			if (e0 == null) {
				error(t, " no expression found");
			}
			else {
				if(isKind(KW_do)) {
					consume();
					Block block = block();
					if(isKind(KW_end)) {
						consume();
						s0 = new StatWhile(t, e0, block);
					}
					else {
						error(t, " missing end");
					}
				}
				else {
					error(t, " missing do");
				}
			}
		}
		
		else if(isKind(KW_repeat)) {
			consume();
			Block block = block();
			if(isKind(KW_until)) {
				consume();
				Exp e0 = exp();
				if (e0 == null) {
					error(t, " no expression found");
				}
				else {
					s0 = new StatRepeat(t, block, e0);
				}
			}
			else {
				error(t, " missing until keyword");
			}
		}
		
		else if(isKind(KW_if)) {
			List<Exp> es = new ArrayList<Exp>();
			List<Block> bs = new ArrayList<Block>();
			consume();
			Exp e0 = exp();
			if (e0 == null) {
				error(t, " no expression found");
			}
			else {
				es.add(e0);
				if(isKind(KW_then)) {
					consume();
					Block b0 = block();
					bs.add(b0);
					while(isKind(KW_elseif)) {
						consume();
						Exp e1 = exp();
						if (e1 == null) {
							error(t, " no expression found");
						}
						else {
							es.add(e1);
							if(isKind(KW_then)) {
								consume();
								Block b1 = block();
								bs.add(b1);
							}
							else {
								error(t, " missing keyword then");
							}
						}
					}
					if(isKind(KW_else)) {
						consume();
						Block b2 = block();
						bs.add(b2);
						
						if(isKind(KW_end)) {
							consume();
							s0 = new StatIf(t, es, bs);
						}
						else {
							error(t, " missing keyword end");
						}
					}
					else {
						if(isKind(KW_end)) {
							consume();
							s0 = new StatIf(t, es, bs);
						}
						else {
							error(t, " missing keyword end");
						}
					}
				}
				else {
					error(t, " missing then keyword");
				}
			}
		}
		
		else if(isKind(KW_for)) {
			consume();
			if (isKind(NAME)) {
				ExpName e0 = new ExpName(t);
				consume();
				if (isKind(ASSIGN)) {
					consume();
					Exp ebeg = exp();
					if(ebeg == null) {
						error(t, " no expression found");
					}
					else {
						if(isKind(COMMA)) {
							consume();
							Exp eend = exp();
							if(eend == null) {
								error(t, " no expression found");
							}
							else {
								Exp einc = null;
								if(isKind(COMMA)) {
									consume();
									einc = exp();
									if(einc == null) {
										error(t, " no expression found");
									}
								}
								if(isKind(KW_do)) {
									consume();
									Block b1 = block();
									if(isKind(KW_end)) {
										consume();
										s0 = new StatFor(t, e0, ebeg, eend, einc, b1); 
									}
									else {
										error(t, " missing end keyword");
									}
								}
								else {
									error(t, " missing do keyword");
								}
							}
						}
						else {
							error(t, " missing comma");
						}
					}	
				}
				else if(isKind(COMMA) || isKind(KW_in)) {
					List<ExpName> names = new ArrayList<ExpName>();
					names.add(e0);
					while(isKind(COMMA)) {
						consume();
						if(isKind(NAME)) {
							names.add(new ExpName(t));
							consume();
						}
						else {
							error(t, " missing identifier");
						}
					}
					if(isKind(KW_in)) {
						consume();
						List<Exp> exps = expListArgs();
						if(isKind(KW_do)) {
							consume();
							Block block = block();
							if(isKind(KW_end)) {
								consume();
								s0 = new StatForEach(t, names, exps, block);
							}
							else {
								error(t, " missing keyword end");
							}
						}
						else {
							error(t, " missing keyword do");
						}
					}
					else {
						error(t, " missing keyword in");
					}
				}
				else {
					error(t, " only assign or comma allowed here");
				}
			}
			else {
				error(t, " missing identifier");
			}
		}
		
		else if(isKind(KW_function)) {
			consume();
			if(isKind(NAME)) {
				FuncName name = FuncName();
				FuncBody body = FuncBody();
				s0 = new StatFunction(t, name, body);
			}
			else {
				error(t, " missing identifier");
			}
		}
		
		else if(isKind(KW_local)) {
			consume();
			if(isKind(KW_function)) {
				consume();
				if(isKind(NAME)) {
					FuncName name = FuncName();
					FuncBody body = FuncBody();
					s0 = new StatLocalFunc(t, name, body);
				}
				else {
					error(t, " missing identifier");
				}
			}
			else if(isKind(NAME)) {
				List<ExpName> names = new ArrayList<ExpName>();
				ExpName e0 = new ExpName(t);
				consume();
				names.add(e0);
				while(isKind(COMMA)) {
					consume();
					if(isKind(NAME)) {
						names.add(new ExpName(t));
						consume();
					}
					else {
						error(t, " missing identifier");
					}
				}
				List<Exp> expList = null;
				if(isKind(ASSIGN)) {
					consume();
					expList = expListArgs();
				}
				s0 = new StatLocalAssign(t, names, expList);
				
			}
			else {
				error(t, " only function or name allowed after local keyword");
			}
		}
		
		
		return s0;
	}

	private Block block() throws Exception {
		List<Stat> stats = new ArrayList<Stat>();
		Stat s0 = Statement();
		while(flagSEMI) {
			s0 = Statement();
		}
		while(s0 != null) {
			stats.add(s0);
			s0 = Statement();
			while(flagSEMI) {
				s0 = Statement();
			}
		
		}
		if(isKind(KW_return)) {
			consume();
			List<Exp> e0 = new ArrayList<Exp>();
			if(!isKind(KW_end) & !isKind(SEMI) & !isKind(KW_until) & !isKind(KW_elseif) & !isKind(KW_else)) {
				e0 = expListArgs2();
			}
			if(isKind(SEMI)) {
				consume();
			}
			stats.add(new RetStat(t, e0));
		}
		Block block = new Block(t, stats);
		return block;
	}

	private Chunk chunk() throws Exception {
		Chunk c0 = new Chunk(t, block());
		return c0;
	}
	
	public Chunk parse() throws Exception {
		Chunk chunk = chunk();
		if (!isKind(EOF)) throw new SyntaxException(t, "Parse ended before end of input");
		return chunk;
	}
	
	protected boolean isKind(Kind kind) {
		return t.kind == kind;
	}

	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind)
				return true;
		}
		return false;
	}

	/**
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	Token match(Kind kind) throws Exception {
		Token tmp = t;
		if (isKind(kind)) {
			consume();
			return tmp;
		}
		error(kind);
		return null; // unreachable
	}

	/**
	 * @param kind
	 * @return
	 * @throws Exception
	 */
	Token match(Kind... kinds) throws Exception {
		Token tmp = t;
		if (isKind(kinds)) {
			consume();
			return tmp;
		}
		StringBuilder sb = new StringBuilder();
		for (Kind kind1 : kinds) {
			sb.append(kind1).append(kind1).append(" ");
		}
		error(kinds);
		return null; // unreachable
	}

	Token consume() throws Exception {
		
		Token tmp = t;
		
		t = scanner.getNext();
		

		return tmp;
	}
	
	void error(Kind... expectedKinds) throws SyntaxException {
		String kinds = Arrays.toString(expectedKinds);
		String message;
		if (expectedKinds.length == 1) {
			message = "Expected " + kinds + " at " + t.line + ":" + t.pos;
		} else {
			message = "Expected one of" + kinds + " at " + t.line + ":" + t.pos;
		}
		throw new SyntaxException(t, message);
	}

	void error(Token t, String m) throws SyntaxException {
		String message = m + " at " + t.line + ":" + t.pos;
		throw new SyntaxException(t, message);
	}
	


}
