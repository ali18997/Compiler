package interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import static cop5556fa19.Token.Kind.*;
//import cop5556fa19.BuildSymbolTable;
import cop5556fa19.Parser;
import cop5556fa19.Scanner;
import cop5556fa19.Token.Kind;
import cop5556fa19.Token;
import cop5556fa19.AST.*;
import cop5556fa19.Parser.SyntaxException;
import interpreter.built_ins.print;
import interpreter.built_ins.println;
import interpreter.built_ins.toNumber;
import interpreter.StaticSemanticException;

public class Interpreter extends ASTVisitorAdapter{



	
	LuaTable _G; //global environment

	/* Instantiates and initializes global environment
	 * 
	 * Initially, the "standard library" routines implemented in Java are loaded.  For this assignment,
	 * this is just print and println.  
	 * 
	 * These functions impl
	 */
	
	public List<Integer> loopIfList = new ArrayList<>();
	
	void init_G() {
		_G = new LuaTable();
		_G.put("print", new print());
		_G.put("println", new println());
		_G.put("toNumber", new toNumber());
	}
	
	ASTNode root; //useful for debugging
		
	public Interpreter() {
		init_G();
	}
	

	public Chunk chunkMain;
	@SuppressWarnings("unchecked")
	public List<LuaValue> load(Reader r) throws Exception {
		Scanner scanner = new Scanner(r); 
		Parser parser = new Parser(scanner);
		Chunk chunk = parser.parse();
		root = chunk;

		chunkMain = chunk;
		List<LuaValue> LuaList = checkBlock(chunk.block);
		while(passAll) {
			passAll = false;
			LuaList.addAll(checkBlock(chunk.block));
		}
		if(gotoFlag) {
			throw new interpreter.StaticSemanticException(parser.t, "No label found");
		}
		if(LuaList.isEmpty()) {
			LuaList = null;
		}
		//Perform static analysis to prepare for goto.  Uncomment after u
//		StaticAnalysis hg = new StaticAnalysis();
//		chunk.visit(hg,null);	
		//Interpret the program and return values returned from chunk.visit
		
		//List<LuaValue> vals = (List<LuaValue>) chunk.visit(this,_G);
		
		//return vals;
		
		return LuaList;
	}
	
	Boolean returnFlag = false;
	Boolean breakFlag = false;
	Boolean gotoFlag = false;
	String GotoLabel = "";
	Boolean passAll = false;
	
	public List<LuaValue> checkBlock(Block b) throws Exception{
		
		List<LuaValue> LuaList = new ArrayList<LuaValue>();
		List<Stat> statements = b.stats;
		for (int i = 0; i < statements.size(); i++) {
			
			LuaList.addAll(checkStat(statements.get(i)));
			if(returnFlag) {
				break;
			}
			if(breakFlag) {
			
				if(loopIfList.get(loopIfList.size()-1) == 2) {
					Boolean loopExists = false;
					for(int j = loopIfList.size()-1; j >=0; j--) {
						if(loopIfList.get(j) == 1) {
							loopExists = true;
							break;
						}
					}
					if(loopExists) {
						break;
					}
					else {
						breakFlag = false;
						break;
					}
				}
				else if(loopIfList.get(loopIfList.size()-1) == 1) {
					break;
				}
			}
			
		}
		return LuaList;
	}
	
	public List<LuaValue> checkStat(Stat s) throws Exception{
		List<LuaValue> LuaList = new ArrayList<LuaValue>();
		if(s.getClass() == RetStat.class && !gotoFlag && !passAll) {
			RetStat r = (RetStat) s;
			returnFlag = true;
			List<Exp> exps = r.el;
			for (int i = 0; i < exps.size(); i++) {
				Exp current = exps.get(i);
				
				
				if(current.getClass() == ExpName.class) {
					LuaList.add(_G.get(current.toString()));
				}
				else {
					LuaList.add(checkExp(current));
				}
				
			}
		}
		else if(s.getClass() == StatAssign.class && !gotoFlag && !passAll) {
			StatAssign r = (StatAssign) s;
			List<Exp>  varList = r.varList;
			List<Exp>  expList = r.expList;
			for (int i = 0; i < varList.size(); i++) {
				if(varList.get(i).getClass() == ExpName.class) {
					_G.put(varList.get(i).toString(), checkExp(expList.get(i)));
				}
				else if(varList.get(i).getClass() == ExpTableLookup.class) {
					ExpTableLookup r1 = (ExpTableLookup) varList.get(i);
					LuaTable table = (LuaTable)_G.get(r1.table.toString());
					LuaValue luaKey = checkExp(r1.key);
					table.put(luaKey, checkExp(expList.get(i)));
				}
			}
		}
		
		else if(s.getClass() == StatDo.class && !gotoFlag && !passAll) {
			loopIfList.add(2);
			StatDo r = (StatDo) s;
			LuaList.addAll(checkBlock(r.b));
			loopIfList.remove(loopIfList.size()-1);
		}
		
		else if(s.getClass() == StatIf.class && !gotoFlag && !passAll) {
			loopIfList.add(2);
			StatIf r = (StatIf) s;
			int length = r.es.size();
			Boolean tempFlag = false;
			
			for (int i = 0; i < length; i++) {
				LuaValue L = checkExp(r.es.get(i));
				if(r.es.get(i).getClass() == ExpTrue.class || r.es.get(i).getClass() == ExpInt.class) {
					
					LuaList.addAll(checkBlock(r.bs.get(i)));
					tempFlag = true;
					break;
				}
				else if(L != null){
					if(L.getClass() == LuaInt.class) {
						LuaList.addAll(checkBlock(r.bs.get(i)));
						tempFlag = true;
						break;
					}
					else if(L.getClass() != LuaNil.class) {	
						if(((LuaBoolean)checkExp(r.es.get(i))).value) {
							LuaList.addAll(checkBlock(r.bs.get(i)));
							tempFlag = true;
							break;
						}
					}
				}
				
			}
			if(!tempFlag && r.es.size() < r.bs.size()) {
			
				LuaList.addAll(checkBlock(r.bs.get(r.es.size())));
			}
			
			loopIfList.remove(loopIfList.size()-1);
		}
		
		else if(s.getClass() == StatWhile.class && !gotoFlag && !passAll) {
			loopIfList.add(1);
			
			StatWhile r = (StatWhile) s;
			Exp e = r.e;
			Block b = r.b;
		
			while(((LuaBoolean)checkExp(e)).value) {
				LuaList.addAll(checkBlock(b));
				if(breakFlag) {
					break;
				}
			}
			breakFlag = false;
			loopIfList.remove(loopIfList.size()-1);
		}
		
		else if(s.getClass() == StatRepeat.class && !gotoFlag && !passAll) {
			loopIfList.add(1);
			
			StatRepeat r = (StatRepeat) s;
			Exp e = r.e;
			Block b = r.b;
		
			LuaList.addAll(checkBlock(b));
			while(!((LuaBoolean)checkExp(e)).value) {
				
				LuaList.addAll(checkBlock(b));
				if(breakFlag) {
					break;
				}
			}
			breakFlag = false;
			loopIfList.remove(loopIfList.size()-1);
		}
		
		else if(s.getClass() == StatBreak.class && !gotoFlag && !passAll) {
			
			breakFlag = true;
		}
		
		else if(s.getClass() == StatGoto.class && !gotoFlag && !passAll) {
			StatGoto r = (StatGoto) s;
			GotoLabel = r.name.name;
			gotoFlag = true;
			passAll = true;
			
		}
		
		else if(s.getClass() == StatLabel.class && gotoFlag) {
			
			StatLabel r = (StatLabel) s;
			if(GotoLabel.matches(r.label.name)) {
				gotoFlag = false;
				passAll = false;
			}
		}
		
		return LuaList;
	}
	
	public LuaValue checkExp(Exp e) throws Exception {
	
		if(e.getClass() == ExpInt.class) {
			ExpInt ei = (ExpInt) e;
			int val = ei.v;
			return new LuaInt(val);
		}
		else if(e.getClass() == ExpName.class) {
			return _G.get(e.toString());
		}
		else if(e.getClass() == ExpFunctionCall.class) {
			ExpFunctionCall ef = (ExpFunctionCall) e;
			if(ef.f.getClass() == ExpName.class) {
				String name = ((ExpName)ef.f).name;
				List<Exp> argList = ef.args;
				List<LuaValue> argLuaList = new ArrayList<LuaValue>();
				for (int i = 0; i < argList.size(); i++) {
					argLuaList.add(checkExp(argList.get(i)));
				}
				if(name.matches("print")) {
					print a = new print();
					a.call(argLuaList);
				}
				else if(name.matches("println")) {
					println a = new println();
					a.call(argLuaList);
				}
				else if(name.matches("toNumber")) {
					toNumber a =  new toNumber();
					return a.call(argLuaList).get(0);
				}
			}
			return null;
		}
		else if(e.getClass() == ExpTrue.class) {
			return new LuaBoolean(true);
		}
		else if(e.getClass() == ExpFalse.class) {
			return new LuaBoolean(false);
		}
		else if(e.getClass() == ExpString.class) {
			ExpString eb = (ExpString) e;
			return new LuaString(eb.v);
		}
		else if(e.getClass() == ExpBinary.class){
			ExpBinary eb = (ExpBinary) e;
			LuaValue LV1 = checkExp(eb.e0);
			LuaValue LV2 = checkExp(eb.e1);
			Kind op = eb.op;
			
			if(LV1.getClass() == LuaInt.class && LV2.getClass() == LuaInt.class) {
				LuaInt LV1int = (LuaInt) LV1;
				LuaInt LV2int = (LuaInt) LV2;
				if(op == OP_PLUS) {
					return new LuaInt(LV1int.v + LV2int.v);
				}
				else if(op == OP_POW) {	
					return new LuaInt((int)Math.pow(LV1int.v, LV2int.v));
				}
				else if(op == OP_MINUS) {
					return new LuaInt(LV1int.v - LV2int.v);
				}
				else if(op == OP_TIMES) {
					return new LuaInt(LV1int.v * LV2int.v);
				}
				else if(op == OP_DIV) {
					return new LuaInt(LV1int.v / LV2int.v);
				}
				else if(op == OP_MOD) {
					return new LuaInt(LV1int.v % LV2int.v);
				}
				else if(op == REL_LE) {
					return new LuaBoolean(LV1int.v <= LV2int.v);
				}
				else if(op == REL_GE) {
					return new LuaBoolean(LV1int.v >= LV2int.v);
				}
				else if(op == REL_LT) {
					return new LuaBoolean(LV1int.v < LV2int.v);
				}
				else if(op == REL_GT) {
					return new LuaBoolean(LV1int.v > LV2int.v);
				}
				else if(op == REL_EQEQ) {
					return new LuaBoolean(LV1int.v == LV2int.v);
				}
				else if(op == REL_NOTEQ) {
					return new LuaBoolean(LV1int.v != LV2int.v);
				}
				else if(op == OP_DIVDIV) {
					return new LuaInt((int) Math.floorDiv(LV1int.v, LV2int.v));
				}
				else if(op == BIT_AMP) {
					return new LuaInt(LV1int.v & LV2int.v);
				}
				else if(op == BIT_OR) {
					return new LuaInt(LV1int.v | LV2int.v);
				}
				else if(op == BIT_XOR) {
					return new LuaInt(LV1int.v ^ LV2int.v);
				}
				else if(op == BIT_SHIFTL) {
					return new LuaInt(LV1int.v << LV2int.v);
				}
				else if(op == BIT_SHIFTR) {
					return new LuaInt(LV1int.v >> LV2int.v);
				}
				else {
					throw new StaticSemanticException(null, "Wrong operator for Integers");
				}
			}
			else if(LV1.getClass() == LuaString.class && LV2.getClass() == LuaString.class) {
				LuaString LV1String = (LuaString) LV1;
				LuaString LV2String = (LuaString) LV2;
				if(op == DOTDOT) {
					return new LuaString(LV1String.value + LV2String.value);
				}
				else if(op == REL_EQEQ) {
					return new LuaBoolean(LV1String.value.matches(LV2String.value));
				}
				else if(op == REL_NOTEQ) {
					return new LuaBoolean(!LV1String.value.matches(LV2String.value));
				}
				else if(op == REL_LE) {
					return new LuaBoolean(LV1String.value.charAt(0) <= LV2String.value.charAt(0));
				}
				else if(op == REL_GE) {
					return new LuaBoolean(LV1String.value.charAt(0) >= LV2String.value.charAt(0));
				}
				else if(op == REL_LT) {
					return new LuaBoolean(LV1String.value.charAt(0) < LV2String.value.charAt(0));
				}
				else if(op == REL_GT) {
					return new LuaBoolean(LV1String.value.charAt(0) > LV2String.value.charAt(0));
				}
				else {
					throw new StaticSemanticException(null, "Wrong operator for Strings");
				}
			}
			
			else if(LV1.getClass() == LuaInt.class && LV2.getClass() == LuaString.class) {
				LuaString LV1String = new LuaString(Integer.toString(((LuaInt) LV1).v));
				LuaString LV2String = (LuaString) LV2;
				if(op == DOTDOT) {
					return new LuaString(LV1String.value + LV2String.value);
				}
			}
			else if(LV2.getClass() == LuaInt.class && LV1.getClass() == LuaString.class) {
				LuaString LV2String = new LuaString(Integer.toString(((LuaInt) LV2).v));
				LuaString LV1String = (LuaString) LV1;
				if(op == DOTDOT) {
					return new LuaString(LV1String.value + LV2String.value);
				}
			}
			else if(LV1.getClass() == LuaBoolean.class && LV2.getClass() == LuaBoolean.class) {
				LuaBoolean LV1Boolean = (LuaBoolean) LV1;
				LuaBoolean LV2Boolean = (LuaBoolean) LV2;
				if(op == KW_and) {
					return new LuaBoolean(LV1Boolean.value && LV2Boolean.value);
				}
				else if(op == KW_or) {
					return new LuaBoolean(LV1Boolean.value || LV2Boolean.value);
				}
				else {
					throw new StaticSemanticException(null, "Wrong operator for Booleans");
				}
				
			}
			else {
				throw new StaticSemanticException(null, "Wrong combination of types in binary expression");
			}
		}
		
		else if(e.getClass() == ExpTable.class) {
			ExpTable r = (ExpTable) e;
			LuaTable t = new LuaTable();
			List<Field> fieldList = r.fields;
			
			for(int i = 0; i < fieldList.size(); i++) {
				
				if(fieldList.get(i).getClass() == FieldImplicitKey.class) {
					FieldImplicitKey fk = (FieldImplicitKey) fieldList.get(i);
					LuaValue a = checkExp(fk.exp);
					if (a != null) {
						t.putImplicit(checkExp(fk.exp));
					}	
				}
				else if(fieldList.get(i).getClass() == FieldExpKey.class) {
					FieldExpKey fk = (FieldExpKey) fieldList.get(i);
					Exp key = fk.key;
					Exp value = fk.value;
					t.put(checkExp(key), checkExp(value));
				}
				else if(fieldList.get(i).getClass() == FieldNameKey.class) {
					FieldNameKey fk = (FieldNameKey) fieldList.get(i);
					Name key = fk.name;
					Exp value = fk.exp;
					t.put(key.name, checkExp(value));
				}
				
			}
			
			return t;
		}
		
		else if(e.getClass() == ExpUnary.class) {
			ExpUnary r = (ExpUnary) e;
			LuaValue ex = checkExp(r.e);
			Kind op = r.op;
			if(op == OP_MINUS) {
				if(ex.getClass() == LuaInt.class) {
					return new LuaInt(-((LuaInt) ex).v);
				}
				else {
					throw new StaticSemanticException(null, "Wrong value in unary expression with minus");
				}
			}
			else if(op == OP_HASH) {
				if(ex.getClass() == LuaString.class) {
					return new LuaInt((((LuaString) ex).value).length());
				}
				else {
					throw new StaticSemanticException(null, "Wrong value in unary expression with hash");
				}
			}
			else if(op == KW_not) {
				if(r.e.getClass() == ExpTrue.class || ex.getClass() == LuaInt.class || ex.getClass() == LuaString.class) {
					return new LuaBoolean(false);
				}
				else if(r.e.getClass() == ExpFalse.class) {
					return new LuaBoolean(true);
				}
				else {
					throw new StaticSemanticException(null, "Wrong value in unary expression with not");
				}
			}
			else if(op == BIT_XOR) {
				if(ex.getClass() == LuaInt.class) {
					return new LuaInt((((LuaInt)ex).v*-1)-1);
				}
				else {
					throw new StaticSemanticException(null, "Wrong value in unary expression with xor");
				}
			}
			
		}
		
		return null;
	}
	


	

}
