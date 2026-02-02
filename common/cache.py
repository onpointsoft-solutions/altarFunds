from django.core.cache import cache
from functools import wraps
import hashlib
import json

def cache_response(timeout=300, key_prefix=''):
    """Cache decorator for API responses"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            cache_key = f"{key_prefix}:{func.__name__}:{hashlib.md5(json.dumps(str(args) + str(kwargs)).encode()).hexdigest()}"
            result = cache.get(cache_key)
            if result is None:
                result = func(*args, **kwargs)
                cache.set(cache_key, result, timeout)
            return result
        return wrapper
    return decorator

def invalidate_cache(key_pattern):
    """Invalidate cache by pattern"""
    cache.delete_pattern(key_pattern)
