import logging, time, copy
import io, gzip
from lxml import etree
import sys

class Parsing_File:

    def __init__( self, file_name ) :
        # Set variables
        self.file_name = file_name
        # Init logger
        self.logger = logging.getLogger( 'clinvar.Parser' )

    def _file_init( self ) :
        pass

    def _get_block( self ) :
        content = None
        # Get line
        line = self.file.readline( )
        self.line +=1
        # End of file: line == "" -> return None
        if line != "":
            content = io.StringIO( )
            print (line, file=content)
        return content

    def _process_block( self, content ) :
        # Split by spaces
        return [tuple( content.rstrip().split() )]

    def get_metadata( self ):
        return( time.strftime("%Y-%m-%d") )

    def get_batch( self, batch_size ) :
        self.batch_size = batch_size
        self.file = None
        self.line = 0
        # Unzip and open file
        with gzip.open( self.file_name, "rb" ) as self.file :
            # Preparse init
            self._file_init( )
            list_of_tuples = [ ]
            # Start of file parse
            while True:
                block = None
                try:
                    # Get file block
                    block = self._get_block( )
                    # End of file
                    if block == None :
                        yield  list_of_tuples
                        break
                    # Accumulate values batch
                    list_of_tuples += self._process_block( block.getvalue( ) )
                # Any error while work with block
                except:
                    self.logger.error( 'In line {} (set {})'.
                                        format( self.line, len( list_of_tuples ) + 1 ) 
                                    )
                    raise
                # Yield batch
                if len( list_of_tuples ) >= self.batch_size :
                    yield list_of_tuples
                    del list_of_tuples[:]
        self.file = None

# XML parser
class XML_File( Parsing_File ):

    def __init__( self, file_name, ref_paths, asr_paths, tables_map ) :
        Parsing_File.__init__( self, file_name )
        # Additional variable
        self.ref_paths     = ref_paths # ReferenceClinVarAssertion paths
        self.asr_paths     = asr_paths # ClinVarAssertion paths
        self.tables_map    = tables_map

    def _get_block( self ) :
        content = None
        # Accumulate block
        while True :
            line = self.file.readline( ).decode('utf-8')
            self.line +=1
            if line == "" :
                # End of file
                break
            elif "<ClinVarSet ID=" in line :
                # Start of ClinVar Set Block
                assert content is None
                content = io.StringIO( )
                print (line.rstrip( ), file=content)
            elif "</ClinVarSet>" in line :
                # End of ClinVar Set Block
                print (line.rstrip( ), file=content)
                break
            elif content :
                # Body of ClinVar Set Block
                print (line.rstrip( ), file=content)
        # Return block
        return content

    def _process_block( self, content ) : 
        # Init XML parser
        parser = etree.XMLParser(remove_comments = True, encoding='utf-8')
        root = etree.fromstring(content,parser)
        # Take referense values
        nodes = root.xpath( "/ClinVarSet/ReferenceClinVarAssertion" )
        if len( nodes ) != 1 :
            self.logger.error( '{} ReferenceClinVarAssertion elements'.format( len( nodes ) ) )
            raise( BaseException( "Unnormal ReferenceClinVarAssertion element" ) )
        ref_list = [ ] # list of strings
        for n in nodes :
            for path in self.ref_paths :
                # For XML attribute values
                if type( path ) == tuple :
                    subn = n.xpath( path[ 0 ] )
                    if len( subn ) > 1 :
                        self.logger.error( '{} ReferenceClinVarAssertion nodes'.
                                            format( len( subn ) ) 
                                        )
                        raise( BaseException( "Multiple ReferenceClinVarAssertion nodes" ) )
                    elif len( subn) :
                        ref_list += [ subn[ 0 ].get( path[ 1 ] ) ]
                # For XML content values
                else :
                    subn = n.xpath( path )
                    if len( subn ) > 1 :
                        self.logger.error( '{} ReferenceClinVarAssertion nodes'.
                                            format( len( subn ) ) 
                                        )
                        raise( BaseException( "Multiple ReferenceClinVarAssertion nodes" ) )
                    elif len( subn) :
                        ref_list += [ subn[ 0 ].text ]
        # Take assertion values
        nodes = root.xpath( "/ClinVarSet/ClinVarAssertion" )
        asr_list = [ ] # list of lists
        for n in nodes :
            int_list = [ ] # list of strings
            for path in self.asr_paths :
                # For XML attribute values
                if type( path ) == tuple :
                    subn = n.xpath( path[ 0 ] )
                    if len( subn ) > 1 :
                        self.logger.error( '{} ClinVarAssertion nodes'.format( len( subn ) ) )
                        raise( BaseException( "Multiple ClinVarAssertion nodes" ) )
                    elif len( subn) :
                        int_list += [ subn[ 0 ].get( path[ 1 ] ) ]
                # For XML content values
                else :
                    subn = n.xpath( path )
                    if len( subn ) > 1 :
                        self.logger.error( '{} ClinVarAssertion nodes'.format( len( subn ) ) )
                        raise( BaseException( "Multiple ClinVarAssertion nodes" ) )
                    elif len( subn ) :
                        int_list += [ subn[ 0 ].text ]
            asr_list += [ int_list ]
        # Form return list
        return_list = [ ]               # list of assert's (1)lists
        for l in asr_list :             # for every assertion in block
            tuple_list = [ ]            # (1)lists of table's (2)tuples
            for t in self.tables_map :  # for every table in map
                table_list = [ ]        # (2)list of table elements
                for e in t:             # for every element in table
                    if not e[ 0 ] :
                        table_list += [ ref_list[ e[ 1 ] ] ]
                    else :
                        try:
                            table_list += [ l[ e[ 1 ] ] ]
                        except:
                            table_list += [ None ]
                tuple_list += [ tuple( table_list ) ] 
            try :
                for r in return_list :
                    for s1, s2 in zip( r[1], tuple_list[1] ) :
                        if s1.lower() != s2.lower( ) :
                            break
                        raise( BaseException( ) )
            except BaseException :
                continue
            return_list += [ tuple_list ] 
        return return_list

    def get_metadata( self ):
        if "file" in self.__dict__ and self.file:
            raise( BaseException( "Taking metadata during batch generator" ) ) 
        metadata = ""
        with gzip.open( self.file_name, "rb" ) as self.file :
            content = io.StringIO( )
            # Accumulate block
            while True :
                line = self.file.readline( ).decode('utf-8')
                if line == "" :
                    # End of file
                    break
                elif "<ClinVarSet ID=" in line :
                    print ('</ReleaseSet>', file=content)
                    
                    # Start of ClinVar Set
                    break
                else :
                    # Body of Block
                    print (line.rstrip( ))
                    sys.exit()
                    print (line.rstrip( ), file=content)
            # Parse metadata
            parser = etree.XMLParser(remove_comments = True, encoding='utf-8')
            root = etree.fromstring( bytes(content.getvalue( ), encoding='utf-8'), parser )
            # Take referense values
            nodes = root.xpath( "/ReleaseSet" )
            if len( nodes ) != 1 :
                self.logger.error( 'Unnormal amount of ReleaseSet' )
                raise( BaseException( "Unnormal amount of ReleaseSet" ) )
            for n in nodes :
                metadata = n.get( 'Dated' )
        self.file = None
        return metadata

