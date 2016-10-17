#!/usr/bin/python

from argparse import (
    ArgumentParser,
    RawTextHelpFormatter
)
from subprocess import (
    Popen,
    PIPE,
    STDOUT
)
from os import (
    environ
)
from sys import (
    stdin
)
import logging
import shlex


class DemoMode:
    def __init__(self, mode, server, client):
        self.mode = mode
        self.server = server
        self.client = client

    @property
    def mode(self):
        return self.__mode
    
    @property
    def server(self):
        return self.__server
    @property
    def client(self):
        return self.__client

class DebugConfig:
    def __init__(self, server_port, client_port):
        self.server_port = server_port
        self.client_port = client_port

    @property
    def server_port(self):
        return self.__server_port

    @property
    def client_port(self):
        return self.__client_port

class ExecutionConfig:
    def __init__(self, classpath, environment, servers, clients):
        self.classpath = classpath
        self.environment = environment
        self.servers = servers
        self.clients = clients

    @property
    def classpath(self):
        return self.__classpath

    @property
    def environment(self):
        return self.__environment

    @property
    def servers(self):
        return self.__servers

    @property
    def clients(self):
        return self.__clients

        
class Execution:
    def __init__(self, demo_mode, server_param, client_param, execution_config, debug_config, logger):
        self.demo_mode = demo_mode
        self.server_param = server_param
        self.client_param = client_param
        self.execution_config= execution_config
        self.debug_config = debug_config
        self.logger = logger

    @property
    def demo_mode(self):
        return self.__demo_mode
        
    @property
    def server_param(self):
        return self.__server_param
        
    @property
    def client_param(self):
        return self.__client_param

    @property
    def debug_config(self):
        return self.__debug_config

    @property
    def execution_config(self):
        return self.__execution_config
        
    @property
    def logger(self):
        return self.__logger

    def start_servers(self):
        self.logger.debug("Starting %d servers..." % (self.execution_config.servers))
        for id in range (0, self.execution_config.servers):
            command = self.assemble_server_command(id)
            self.logger.debug("Executing '%s'." % (command))
            self.run_command_in_shell(command)

    def start_clients(self):
        self.logger.debug("Starting %d clients..." % (self.execution_config.clients))
        for id in range (0, self.execution_config.clients):
            command = self.assemble_client_command(id)
            self.logger.debug("Executing '%s'." % (command))
            self.run_command_in_shell(command)

    def run_command_in_shell(self, command):
        cur_env = environ.copy()
        bash = Popen(shlex.split('''lxterm -e "(cd %s && %s) || read -p ' ### Execution aborted. Press any key. ### '"''' % (self.execution_config.environment, command)), stdout=PIPE, stderr=STDOUT, env=cur_env)
        #bash = Popen(shlex.split('lxterm -e "/bin/bash -c %s"' % (command)), stdin=PIPE)
        

    def assemble_server_command(self, id):
        debug_string = ""
        
        if self.debug_config is not None:
            debug_string = self.assemble_debug_argument(self.debug_config.server_port + id)

        return self.assemble_command(id, self.server_param, debug_string, self.demo_mode.server)
        
    def assemble_client_command(self, id):
        debug_string = ""        
        if self.debug_config is not None:
            debug_string = self.assemble_debug_argument(self.debug_config.client_port + id)

        return self.assemble_command(id, self.client_param, debug_string, self.demo_mode.client)

    def assemble_debug_argument(self, port):
        return "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=%d" % (port)
            
    def assemble_command(self, id, param, debug_string, main_class, ):
        return "java -cp %s %s %s %d %s" % (self.execution_config.classpath, debug_string, main_class, id, " ".join(param))

    def execute(self):
        self.start_servers()
        raw_input("Give me any key to start the clients...")
        #stdin.readline()
        self.start_clients()



def parse_bftmap(args, logger, execution_config, debug_config, demo_modes):
    execution = Execution(demo_modes['bftmap'], [], [], execution_config, debug_config, logger)
    execution.execute()

def parse_counter(args, logger, execution_config, debug_config, demo_modes):
    execution = Execution(demo_modes['counter'], [], ['%d' % (args.increment), '%d' % (args.operations)], execution_config, debug_config, logger)
    execution.execute()    

def parse_latency(args, logger, execution_config, debug_config, demo_modes):
    execution = Execution(demo_modes['latency'], ['%d' % (args.measurementInterval), '%d' % (args.replySize)], ['%d' % (args.operations), '%d' % (args.requestSize), '%d' % (args.interval), '%s' % (args.readOnly)], execution_config, debug_config, logger)
    execution.execute()    

def parse_throughput(args, logger, execution_config, debug_config, demo_modes):
    execution = Execution(demo_modes['throghput'], ['%d' % (args.measurementInterval), '%d' % (args.replySize), '%d' % (args.stateSize), '%s' % (args.context)], ['%d' % (args.threads), '%d' % (args.operations), '%d' % (args.requestSize), '%d' % (args.interval), '%s' % (args.readOnly), '%s' % (args.verbose), '%s' % (args.dos)], execution_config, debug_config, logger)
    execution.execute()    



def setup_parser(logger, demo_modes):
    help_title = 'Bootstrapping utility for BFT-SMaRt demos'
    parser_description = help_title + '\n\nDemos:\n\t' + '\n\t'.join('%s' % (v.mode) for k, v in demo_modes.iteritems())

    parser = ArgumentParser(description=parser_description, formatter_class=RawTextHelpFormatter)
    parser.add_argument("-v", "--verbose", dest="verbose", action="store_true", help="More verbose output")
    parser.add_argument("-d", "--debug", dest="debug", action="store_true", help="Set up remote debugging")
    parser.add_argument("-p", "--classpath", dest="classpath", type=str, default="bin/*:lib/*", help="The classpath to execute [default: %(default)s]")
    parser.add_argument("-e", "--environment", dest="environment", type=str, default="~/projects/optSCORE/bft-smart/", help="The execution directory [default: %(default)s]")
    parser.add_argument("--server-port", dest="server_port", type=int, default=5000, help="The first remote debugging port for the servers [default: %(default)d] ") 
    parser.add_argument("--client-port", dest="client_port", type=int, default=6000, help="The first remote debugging port for the clients [default: %(default)d] ")
    parser.add_argument("-r", "--replicas", dest="replicas", type=int, default=4, help="The number of replicas to use [default: %(default)d] ")
    parser.add_argument("-c", "--clients", dest="clients", type=int, default=1, help="The number of clients to use [default: %(default)d] ")
    
    subparsers = parser.add_subparsers(title='demos', description='Some description')
    bftmap_parser = subparsers.add_parser('bftmap', help='BFTMap is a table of tables simulating a database. It creates a HashMap in the server and the replicas maintain the state of the table with the operations executed by the clients.')
    bftmap_parser.set_defaults(func=parse_bftmap)

    counter_parser = subparsers.add_parser('counter', help='CounterServer is an application where clients submits messages to replica, messages are ordered and the throughput is displayed to the user. It has no real application but is useful to verify the system functioning and performance.')
    counter_parser.add_argument("-i", "--increment", dest="increment", type=int, default=1, help="Specifies how much the counter will be incremented, if 0 the request will be read-only [default: %(default)d] ")
    counter_parser.add_argument("-o", "--operations", dest="operations", type=int, default=1000, help="How many operations to perform [default: %(default)d] ")
    counter_parser.set_defaults(func=parse_counter)

    latency_parser = subparsers.add_parser('latency', help='Microbenchmark for measurirng the request latency.')
    latency_parser.add_argument("-q", "--requestSize", dest="requestSize", type=int, default=1024, help="Specifies the size of the request in byte [default: %(default)d] ")
    latency_parser.add_argument("-p", "--replySize", dest="replySize", type=int, default=1024, help="Specifies the size of the response in byte [default: %(default)d] ")
    latency_parser.add_argument("-t", "--stateSize", dest="stateSize", type=int, default=102400, help="Specifies the size of the state in byte [default: %(default)d] ")
    latency_parser.add_argument("-o", "--operations", dest="operations", type=int, default=1000, help="How many operations to perform [default: %(default)d] ")
    latency_parser.add_argument("-m", "--measurementInterval", dest="measurementInterval", type=int, default=100, help="At which intreval measurements are performed [default: %(default)d] ")
    latency_parser.add_argument("-i", "--interval", dest="interval", type=int, default=100, help="Time to sleep between two contigious messages in ms [default: %(default)d] ")
    latency_parser.add_argument("-r", "--readOnly", dest="readOnly", type=str, default="false", help="If the requests are read only [default: %(default)s] ")

    latency_parser.set_defaults(func=parse_latency)
    
    return parser

def setup_logger():
    logger = logging.getLogger(__name__)
    log_format = '%(asctime)-15s %(message)s'
    logging.basicConfig(format=log_format, level=logging.INFO)
    return logger

def setup_demo_modes():
    return {
            'bftmap': DemoMode('bftmap', 'bftsmart.demo.bftmap.BFTMapServer', 'bftsmart.demo.bftmap.BFTMapClient') , 
            'counter': DemoMode('counter', 'bftsmart.demo.counter.CounterServer', 'bftsmart.demo.counter.CounterClient'), 
            # 'listvalue': DemoMode('listvalue', 'bftsmart.demo.listvalue.CounterServer', 'bftsmart.demo.listvalue.LVClient'),
            'latency': DemoMode('latency', 'bftsmart.demo.microbenchmarks.LatencyServer', 'bftsmart.demo.microbenchmarks.LatencyClient'), 
            'throughput': DemoMode('throughput', 'bftsmart.demo.microbenchmarks.ThroughputLatencyServer', 'bftsmart.demo.microbenchmarks.ThroughputLatencyClient'), 
            'random': DemoMode('random', 'bftsmart.demo.random.RandomServer', 'bftsmart.demo.random.RandomClient'), 
            'ycsb' : DemoMode('ycsb', 'bftsmart.demo.ycsb.counter.YCSBServer', 'bftsmart.demo.ycsb.YCSBClient')
    } 

def main():
    logger = setup_logger()
    demo_modes = setup_demo_modes()
    parser = setup_parser(logger, demo_modes)
        
    args = parser.parse_args()
    
    if args.verbose:
        logger.setLevel(logging.DEBUG)
        logger.debug('Setting up logger (loglevel=verbose)')
    
    execution_config = ExecutionConfig(args.classpath, args.environment, args.replicas, args.clients)
    
    debug_config = None
    if (args.debug):
        debug_config = DebugConfig(args.server_port, args.client_port)

    args.func(args, logger, execution_config, debug_config, demo_modes)
        
if __name__ == "__main__":
    main()
