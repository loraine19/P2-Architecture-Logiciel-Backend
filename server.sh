#!/bin/bash

# Script to start the Spring Boot development server with automatic restart
# Usage: ./server.sh [start|stop|restart|status]

case "$1" in
    start|"")
        echo "🚀 Starting Spring Boot development server with auto-restart..."
        echo "📝 DevTools is enabled - changes will trigger automatic restart"
        echo "🔗 Server will be available at: http://localhost:8080"
        echo "⏹️  Press Ctrl+C to stop"
        echo ""
        mvn clean spring-boot:run
        ;;
    stop)
        echo "⏹️  Stopping Spring Boot server..."
        pkill -f "spring-boot:run" || echo "No Spring Boot server process found"
        ;;
    restart)
        echo "🔄 Restarting Spring Boot server..."
        $0 stop
        sleep 2
        $0 start
        ;;
    status)
        if pgrep -f "spring-boot:run" > /dev/null; then
            echo "✅ Spring Boot server is running"
        else
            echo "❌ Spring Boot server is not running"
        fi
        ;;
    *)
        echo "Usage: $0 [start|stop|restart|status]"
        echo ""
        echo "Commands:"
        echo "  start    - Start the development server (default)"
        echo "  stop     - Stop the server"
        echo "  restart  - Restart the server"
        echo "  status   - Check if server is running"
        echo ""
        echo "Examples:"
        echo "  ./server.sh          # Start server"
        echo "  ./server.sh start    # Start server"
        echo "  ./server.sh stop     # Stop server"
        exit 1
        ;;
esac