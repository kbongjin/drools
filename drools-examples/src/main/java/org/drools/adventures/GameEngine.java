package org.drools.adventures;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.lf5.util.StreamUtils;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseConfiguration;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.common.InternalRuleBase;
import org.drools.conf.AssertBehaviorOption;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.mvel2.MVEL;
import org.mvel2.integration.impl.MapVariableResolverFactory;

public class GameEngine {

    StatefulKnowledgeSession ksession;

    ClassLoader              classLoader;

    public void createGame() {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        kbuilder.add( ResourceFactory.newClassPathResource( "model.drl",
                                                            getClass() ),
                      ResourceType.DRL );
        if ( kbuilder.hasErrors() ) {
            System.out.println( kbuilder.getErrors().toString() );
            System.exit( 1 );
        }

        kbuilder.add( ResourceFactory.newClassPathResource( "queries.drl",
                                                            getClass() ),
                      ResourceType.DRL );
        if ( kbuilder.hasErrors() ) {
            System.out.println( kbuilder.getErrors().toString() );
            System.exit( 1 );
        }
        
        kbuilder.add( ResourceFactory.newClassPathResource( "general.drl",
                                                            getClass() ),
                      ResourceType.DRL );
        if ( kbuilder.hasErrors() ) {
            System.out.println( kbuilder.getErrors().toString() );
            System.exit( 1 );
        } 
        
        kbuilder.add( ResourceFactory.newClassPathResource( "response.drl",
                                                            getClass() ),
                      ResourceType.DRL );
        if ( kbuilder.hasErrors() ) {
            System.out.println( kbuilder.getErrors().toString() );
            System.exit( 1 );
        }         
        
        kbuilder.add( ResourceFactory.newClassPathResource( "events.drl",
                                                            getClass() ),
                      ResourceType.DRL );
        if ( kbuilder.hasErrors() ) {
            System.out.println( kbuilder.getErrors().toString() );
            System.exit( 1 );
        }          

        kbuilder.add( ResourceFactory.newClassPathResource( "commands.drl",
                                                            getClass() ),
                      ResourceType.DRL );
        if ( kbuilder.hasErrors() ) {
            System.out.println( kbuilder.getErrors().toString() );
            System.exit( 1 );
        }
        
      

        KnowledgeBaseConfiguration kbaseConf = KnowledgeBaseFactory.newKnowledgeBaseConfiguration();
        kbaseConf.setOption( AssertBehaviorOption.EQUALITY );

        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase( kbaseConf );
        kbase.addKnowledgePackages( kbuilder.getKnowledgePackages() );

        Counter c = new Counter();
        ksession = kbase.newStatefulKnowledgeSession();

        //      final WorkingMemoryFileLogger logger = new WorkingMemoryFileLogger( ksession );
        //      logger.setFileName( "log/ta.log" );

        ksession.setGlobal( "counter",
                            c );

        classLoader = (((InternalRuleBase) ((KnowledgeBaseImpl) kbase).ruleBase).getRootClassLoader());
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader( classLoader );
            Map vars = new HashMap();
            vars.put( "c",
                      c );
            Map<String, Map> map;
            try {
                map = (Map<String, Map>) MVEL.executeExpression( MVEL.compileExpression( new String( StreamUtils.getBytes( getClass().getResource( "data.mvel" ).openStream() ) ) ),
                                                                 vars );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

            for ( Object o : map.get( "rooms" ).values() ) {
                ksession.insert( o );
            }

            for ( Object o : map.get( "doors" ).values() ) {
                ksession.insert( o );
            }

            for ( Object o : map.get( "characters" ).values() ) {
                ksession.insert( o );
            }

            for ( Object o : map.get( "items" ).values() ) {
                ksession.insert( o );
            }

            for ( Object o : map.get( "locations" ).values() ) {
                ksession.insert( o );
            }

            MapVariableResolverFactory f = new MapVariableResolverFactory( map );
            
            String baseStr = "import  org.drools.adventures.*;  import org.drools.adventures.commands.*;\n";
            FactHandle fh = ksession.insert( MVEL.eval( baseStr + "new EnterEvent( characters['hero'], rooms['first floor hallway'] )",
                                             f ) );
            ksession.fireAllRules();
        } finally {
            Thread.currentThread().setContextClassLoader( currentClassLoader );
        }
    }

    public void receiveCommand(UserSession session, List cmd) {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        String baseStr = "import  org.drools.adventures.*;  import org.drools.adventures.commands.*;\n";
        try {
            Thread.currentThread().setContextClassLoader( classLoader );
            Map vars = new HashMap();
            vars.put( "args",
                      cmd );
            MapVariableResolverFactory f = new MapVariableResolverFactory( vars );
            CommandEnum c = (CommandEnum) cmd.get( 0 );
            switch ( c ) {
                case MOVE : {
                    ksession.insert( new Request(  session, MVEL.eval( baseStr + "new MoveCommand(args[1], args[2])",
                                                f ) ) );
                    ksession.fireAllRules();
                    break;
                }
                case PICKUP : {
                    ksession.insert( new Request(  session, MVEL.eval( baseStr + "new PickupCommand(args[1], args[2])",
                                                f ) ) );
                    ksession.fireAllRules();
                    break;
                }
                case DROP : {
                    ksession.insert( new Request(  session, MVEL.eval( baseStr + "new DropCommand(args[1], args[2])",
                                                f ) ) );
                    ksession.fireAllRules();
                    break;
                }
                case LOOK : {
                    ksession.insert( new Request( session, MVEL.eval( baseStr + "new LookCommand(args[1])",
                                                f ) ) );
                    ksession.fireAllRules();
                    break;
                }          
                case SELECT_CHARACTER : {
                    ksession.insert( new Request( session, MVEL.eval( baseStr + "new SetSessionCharacterCommand(args[1], args[2])",
                                                                      f ) ) );
                                          ksession.fireAllRules();
                                          break;                    
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader( currentClassLoader );
        }

    }

}
