package com.microsoft.bot.builder;


import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 Implements IPropertyAccessor for an IPropertyContainer.
 Note the semantic of this accessor are intended to be lazy, this means teh Get, Set and Delete
 methods will first call LoadAsync. This will be a no-op if the data is already loaded.
 The implication is you can just use this accessor in the application code directly without first calling LoadAsync
 this approach works with the AutoSaveStateMiddleware which will save as needed at the end of a turn.

 <typeparam name="T">type of value the propertyAccessor accesses.</typeparam>
 */
class BotStatePropertyAccessor<T extends Object> implements StatePropertyAccessor<T>
{
    private BotState _botState;

    public BotStatePropertyAccessor(BotState botState, String name)
    {
        _botState = botState;
        setName(name);
    }

    /**
     Gets name of the property.

     <value>
     name of the property.
     </value>
     */
    private String Name;
    public final String getName()
    {
        return Name;
    }
    private void setName(String value)
    {
        Name = value;
    }

    /**
     Delete the property. The semantics are intended to be lazy, note the use of LoadAsync at the start.

     @param turnContext The turn context.
     @return A <see cref="Task"/> representing the asynchronous operation.
     */
    public final CompletableFuture DeleteAsync(TurnContext turnContext)
    {
        return CompletableFuture.runAsync(() -> {
            _botState.LoadAsync(turnContext, false).join();
            _botState.DeletePropertyValueAsync(turnContext, getName()).join();
        });
    }

    /**
     Get the property value. The semantics are intended to be lazy, note the use of LoadAsync at the start.
     ///
     @param turnContext The context object for this turn.
     @param defaultValueFactory Defines the default value. Invoked when no value been set for the requested state property.  If defaultValueFactory is defined as null, the MissingMemberException will be thrown if the underlying property is not set.
     @return A <see cref="Task"/> representing the asynchronous operation.
     */
    public final CompletableFuture<Object> GetAsync(TurnContext turnContext, Supplier<T> defaultValueFactory)
    {
        return CompletableFuture.supplyAsync(() -> {
            _botState.LoadAsync(turnContext, false).join();
            Object obj = _botState.<T>GetPropertyValueAsync(turnContext, getName()).join();
            if (obj == null) {
                // ask for default value from factory
                if (defaultValueFactory == null) {
                    throw new IllegalStateException("Property not set and no default provided.");
                }
                Object result = defaultValueFactory.get();
                // save default value for any further calls
                SetAsync(turnContext, result).join();
                return result;
            }
            return obj;
        });
    }

    /**
     Set the property value. The semantics are intended to be lazy, note the use of LoadAsync at the start.

     @param turnContext turn context.
     @param value value.
     @return A <see cref="Task"/> representing the asynchronous operation.
     */
    public final CompletableFuture SetAsync(TurnContext turnContext, Object value)
    {
        return CompletableFuture.runAsync(() -> {
            _botState.LoadAsync(turnContext, false).join();
            _botState.SetPropertyValueAsync(turnContext, getName(), value).join();
        });
    }
}